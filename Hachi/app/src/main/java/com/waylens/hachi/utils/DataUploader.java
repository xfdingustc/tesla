package com.waylens.hachi.utils;

import android.support.annotation.NonNull;
import android.util.Log;

import com.waylens.hachi.session.SessionManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import crs_svr.CommWaylensParse;
import crs_svr.CrsClientLogin;
import crs_svr.CrsClientStartUpload;
import crs_svr.CrsClientStopUpload;
import crs_svr.CrsClientTranData;
import crs_svr.CrsCommResponse;
import crs_svr.ProtocolCommand;
import crs_svr.ProtocolConstMsg;
import crs_svr.WaylensCommHead;

/**
 *
 * Created by Xiaofei on 2015/3/18.
 */
public class DataUploader {
    private static final String TAG = "DataUploader";
    private static final String DEFAULT_AVATAR_TOKEN = "avatar";
    private static final int BLOCK_SIZE = 1024 * 4;
    private static final int PACKET_HEADER_SIZE = 2;

    private String mAddress;
    private int mPort;
    private String mPrivateKey;

    private UploadListener mListener;
    private Socket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private long mUploadCount;
    private long totalCount = 1;
    private byte[] mTotalSHA1;
    private byte mDataType;
    private String mToken;
    private String mGuid;
    private long mStartTime;

    public DataUploader(String serverAddress, int port, String privateKey) {
        mAddress = serverAddress;
        mPort = port;
        mPrivateKey = privateKey;
        CommWaylensParse.private_key = mPrivateKey;
        CommWaylensParse.encode_type = (byte) ProtocolConstMsg.ENCODE_TYPE_OPEN;
    }

    public interface UploadListener {
        void onUploadStarted();

        void onUploadProgress(float progress);

        void onUploadFinished();

        void onUploadError(String error);
    }

    public void setUploaderListener(UploadListener listener) {
        mListener = listener;
    }

    public void uploadBinary(byte[] data, int dataType, String token, String guid) {
        if (data == null || data.length == 0) {
            return;
        }
        totalCount = data.length;
        mDataType = (byte) dataType;
        mToken = token;
        mTotalSHA1 = null;
        mGuid = guid;
        mInputStream = new ByteArrayInputStream(data);
        uploadImpl();
    }

    public boolean uploadStream(InputStream inputStream, int contentLength, int dataType, String token, String guid) {
        if (inputStream == null) {
            return false;
        }
        totalCount = contentLength;
        mDataType = (byte) dataType;
        mToken = token;
        mTotalSHA1 = null;
        mGuid = guid;
        mInputStream = inputStream;
        return uploadImpl();
    }

    public boolean uploadFile(File file, int dataType, String token, String guid, byte[] header, byte[] tail) {
        if (file == null || !file.exists()) {
            Log.i(TAG, "File does not exist.");
            return false;
        }

        totalCount = file.length();
        mDataType = (byte) dataType;
        if (token != null) {
            mToken = token;
        } else {
            mToken = DEFAULT_AVATAR_TOKEN;
        }
        mGuid = guid;
        mTotalSHA1 = null;

        try {
            mInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            logError(e);
            return false;
        }
        return uploadImpl();
    }

    private boolean uploadImpl() {
        try {
            init();
            login();
            if (!startUpload()) {
                return true;
            }
            doUpload();
            stopUpload();
            return true;
        } catch (Exception e) {
            logError(e);
            return false;
        } finally {
            release();
        }
    }

    private void logError(Exception e) {
        if (mListener != null) {
            mListener.onUploadError("Error: " + e.getMessage());
        }
        Log.d(TAG, "", e);
    }

    private void init() throws IOException {
        mSocket = new Socket(mAddress, mPort);
        mOutputStream = mSocket.getOutputStream();
    }

    private void release() {
        try {
            if (mInputStream != null) {
                mInputStream.close();
            }
            if (mOutputStream != null) {
                mOutputStream.close();
            }
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            Log.d(TAG, "release", e);
        }
    }

    private void login() throws IOException {
        byte[] data = new byte[512];
        WaylensCommHead comm_header = new WaylensCommHead();
        comm_header.version = ProtocolConstMsg.WAYLENS_VERSION;
        comm_header.cmd = ProtocolCommand.CRS_C2S_LOGIN;
        comm_header.size = WaylensCommHead.COMM_HEAD_LEGTH;

        CrsClientLogin login = new CrsClientLogin();
        login.device_id = getDeviceID();
        login.token = mToken;

        String hash = login.device_id + mPrivateKey;
        login.hash_value = HashUtils.MD5String(hash);
        login.device_type = ProtocolConstMsg.DEVICE_VIDIT;
        int length = CommWaylensParse.encode(comm_header, login, data);
        sendData(data, length);
        int ret = receiveData();
        if (ret != ProtocolConstMsg.RES_STATE_OK) {
            throw new IOException("Error, Login return: " + ret);
        }
    }

    private void sendData(byte[] buffer, int bufferSize) throws IOException {
        mOutputStream.write(buffer, 0, bufferSize);
        mOutputStream.flush();
    }

    private int receiveData() throws IOException {
        byte[] data = new byte[PACKET_HEADER_SIZE];
        InputStream input = mSocket.getInputStream();
        int msg_length = input.read(data, 0, PACKET_HEADER_SIZE);
        if (msg_length != PACKET_HEADER_SIZE) {
            logError(new RuntimeException("Error to read response header."));
            return ProtocolConstMsg.RES_STATE_FAIL;
        }
        int packet_size = DigitUtils.hBytes2Short(data);
        byte[] response_cmd = new byte[packet_size];
        response_cmd[0] = data[0];
        response_cmd[1] = data[1];

        input.read(response_cmd, PACKET_HEADER_SIZE, packet_size - PACKET_HEADER_SIZE);

        WaylensCommHead comm_head = new WaylensCommHead();
        CrsCommResponse response = new CrsCommResponse();
        CommWaylensParse.decode(response_cmd, packet_size, comm_head, response);

        Log.i(TAG, "return = " + response.ret + " comm type = " + comm_head.cmd);
        return response.ret;
    }

    private boolean startUpload() throws IOException {
        byte[] data = new byte[512];
        WaylensCommHead comm_header = new WaylensCommHead();
        comm_header.version = ProtocolConstMsg.WAYLENS_VERSION;
        comm_header.cmd = ProtocolCommand.CRS_C2S_START_UPLOAD;
        comm_header.size = WaylensCommHead.COMM_HEAD_LEGTH;

        CrsClientStartUpload start_upload = new CrsClientStartUpload();
        start_upload.device_id = getDeviceID();
        start_upload.token = mToken;
        if (mTotalSHA1 != null) {
            start_upload.file_sha1 = mTotalSHA1;
        }
        start_upload.upload_type = ProtocolConstMsg.HISTORY_DATA;
        start_upload.data_type = mDataType;
        start_upload.file_size = totalCount;

        int length = CommWaylensParse.encode(comm_header, start_upload, data);
        sendData(data, length);
        int ret = receiveData();
        // File already exist, so return false;
        if (ret == ProtocolConstMsg.RES_FILE_TRANS_COMPLETE) {
            if (mListener != null) {
                mListener.onUploadFinished();
            }
            return false;
        }

        if (ret != ProtocolConstMsg.RES_STATE_OK) {
            throw new IOException("Error, startUpload return: " + ret);
        }

        if (mListener != null) {
            mListener.onUploadStarted();
        }
        return true;
    }

    private void doUpload() throws IOException, IllegalStateException {
        byte[] data = new byte[4 * 1024];
        int packetNumber = 0;
        mStartTime = System.currentTimeMillis();
        while (true) {
            int read_bytes_cnt = mInputStream.read(data);
            int second_read_cnt = 0;
            if (read_bytes_cnt > 0 && read_bytes_cnt < data.length) {
                second_read_cnt = mInputStream.read(data, read_bytes_cnt, data.length - read_bytes_cnt);
            }
            if (read_bytes_cnt < 0) {
                break;
            }

            if (second_read_cnt > 0) {
                read_bytes_cnt = read_bytes_cnt + second_read_cnt;
            }
            //mPacketSHA1 = HashUtils.SHA1(data, read_bytes_cnt);
            //uploadPacket(data, read_bytes_cnt, packetNumber++);
            uploadBlock(data, read_bytes_cnt, 0, 0, packetNumber++);
            /*int server_ret = receiveData();
            if (server_ret != BLOCK_UPLOAD_FINISHED_RETURN_VAL) {
                throw new IOException("In upload content: upload packet failed ret =  " + server_ret);
            }*/
        }
    }

    private void stopUpload() throws IOException {
        byte[] data = new byte[1024];
        WaylensCommHead comm_header = new WaylensCommHead();
        comm_header.version = ProtocolConstMsg.WAYLENS_VERSION;
        comm_header.cmd = ProtocolCommand.CRS_C2S_STOP_UPLOAD;
        comm_header.size = WaylensCommHead.COMM_HEAD_LEGTH;

        CrsClientStopUpload upload = new CrsClientStopUpload(getDeviceID(), mToken);
        int len = CommWaylensParse.encode(comm_header, upload, data);
        sendData(data, len);
        int server_ret = receiveData();
        if (server_ret != ProtocolConstMsg.RES_FILE_TRANS_COMPLETE) {
            throw new IOException("Stop uploading failed ret = " + server_ret);
        }
    }

    private void uploadPacket(byte[] packet, int packetSize, int packetNum) throws IOException,
            IllegalStateException {
        int start = 0;
        int end;
        boolean eos = false;
        boolean isFirstBlock = true;
        int blockType;
        int blockNum = 0;

        while (true) {
            end = start + BLOCK_SIZE;
            if (end >= packetSize) {
                end = packetSize;
                eos = true;
            }

            if (isFirstBlock) {
                isFirstBlock = false;
                blockType = 0b01;
                if (eos && packetNum == 0) {
                    blockType = 0b00;
                }
            } else {
                if (eos) {
                    blockType = 0b11;
                } else {
                    blockType = 0b10;
                }
            }

            byte[] data = Arrays.copyOfRange(packet, start, end);
            uploadBlock(data, end - start, blockType, blockNum++, packetNum);
            start += BLOCK_SIZE;
            if (eos) {
                break;
            }
        }
    }

    private void updateProgress(int blockSize) {
        if (mListener != null) {
            mUploadCount += blockSize;
            float percent = 0;
            if (totalCount != 0) {
                percent = (float) mUploadCount * 100 / totalCount;
            }
            long duration = System.currentTimeMillis() - mStartTime;
            float speed = 0;
            if (duration != 0) {
                speed = (mUploadCount * 1.0f) / duration / 1024 * 1000;
            }
            Log.e("test", "Speed: " + speed + " [KB]");
            mListener.onUploadProgress(percent);
        }
    }

    private void uploadBlock(byte[] block, int block_size, int block_type, int block_num,
                             int packet_num) throws IOException {

        byte[] data = new byte[BLOCK_SIZE + 512];

        WaylensCommHead comm_header = new WaylensCommHead();
        comm_header.version = ProtocolConstMsg.WAYLENS_VERSION;
        comm_header.cmd = ProtocolCommand.CRS_UPLOADING_DATA;
        comm_header.size = WaylensCommHead.COMM_HEAD_LEGTH;

        CrsClientTranData tran_data = new CrsClientTranData();
        tran_data.device_id = getDeviceID();
        tran_data.token = mToken;
        if (mTotalSHA1 != null) {
            tran_data.file_sha1 = mTotalSHA1;
        }
        tran_data.block_sha1 = new byte[20]; //mPacketSHA1;
        tran_data.upload_type = ProtocolConstMsg.HISTORY_DATA;
        tran_data.block_num = 0; //block_num | (block_type << 30);
        tran_data.data_type = mDataType;
        tran_data.seq_num = packet_num;
        tran_data.length = (short) block_size;
        tran_data.buf = block;
        int ilen = CommWaylensParse.encode(comm_header, tran_data, data);
        sendData(data, ilen);
        updateProgress(block_size);
    }

    @NonNull
    private String getDeviceID() {
        return SessionManager.getInstance().getUserId() + "/" + mGuid;
    }
}
