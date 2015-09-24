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
 * Created by Xiaofei on 2015/3/18.
 */
public class DataUploader {
    private static final String TAG = "DataUploader";

    private static final String DEFAULT_AVATAR_TOKEN = "avatar";

    private static final int BLOCK_UPLOAD_FINISHED_RETURN_VAL = 1;
    private static final int FILE_UPLOAD_FINISHED_RETURN_VAL = 2;

    private static final int BLOCK_SIZE = 1024 * 4;

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
    private byte[] mPacketSHA1;
    private byte mDataType;
    private String mToken;
    private String mGuid;

    public DataUploader(String serverAddress, int port, String privateKey) {
        mAddress = serverAddress;
        mPort = port;
        mPrivateKey = privateKey;
        CommWaylensParse.private_key = mPrivateKey;
        CommWaylensParse.encode_type = (byte) ProtocolConstMsg.ENCODE_TYPE_AES;
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

    public void uploadFile(File file, int dataType, String token, String guid, byte[] header, byte[] tail) {
        if (file == null || !file.exists()) {
            Log.i(TAG, "File does not exist.");
            return;
        }

        totalCount = file.length();
        mDataType = (byte) dataType;
        if (token != null) {
            mToken = token;
        } else {
            mToken = DEFAULT_AVATAR_TOKEN;
        }
        mGuid =  guid;
        mTotalSHA1 = null;

        try {
            mInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            if (mListener != null) {
                mListener.onUploadError("File not fount");
            }
            return;
        }
        uploadImpl();
    }

    private void uploadImpl() {
        try {
            init();
            login();
            if (!startUpload()) {
                return;
            }
            doUpload();
            stopUpload();
        } catch (Exception e) {
            if (mListener != null) {
                mListener.onUploadError("Error");
            }
            Log.d(TAG, "", e);
        } finally {
            release();
        }
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
        if (ret != 0) {
            throw new IOException("Error, Login return: " + ret);
        }
    }

    private void sendData(byte[] buffer, int bufferSize) throws IOException {
        mOutputStream.write(buffer, 0, bufferSize);
        mOutputStream.flush();
    }

    private int receiveData() throws IOException {
        byte[] data = new byte[1024];
        int packet_size;
        final int PACKET_HEADER_SIZE = 2;
        InputStream input = mSocket.getInputStream();
        int msg_length = input.read(data, 0, PACKET_HEADER_SIZE);
        packet_size = DigitUtils.hBytes2Short(data);
        byte[] response_cmd = new byte[packet_size];
        response_cmd[0] = data[0];
        response_cmd[1] = data[1];

        input.read(data, 0, packet_size - PACKET_HEADER_SIZE);
        for (int i = 0; i < packet_size - PACKET_HEADER_SIZE; i++) {
            response_cmd[i + PACKET_HEADER_SIZE] = data[i];
        }

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
        if (ret == 2) {
            if (mListener != null) {
                mListener.onUploadFinished();
            }
            return false;
        }

        if (ret != 0) {
            throw new IOException("Error, startUpload return: " + ret);
        }

        if (mListener != null) {
            mListener.onUploadStarted();
        }
        return true;
    }

    private void doUpload() throws IOException, IllegalStateException {
        byte[] data = new byte[4 * 1024 * 1024];
        int packetNumber = 0;

        while (true) {
            int read_bytes_cnt = mInputStream.read(data);
            if (read_bytes_cnt < 0) {
                break;
            }
            mPacketSHA1 = HashUtils.SHA1(data, read_bytes_cnt);
            uploadPacket(data, read_bytes_cnt, packetNumber++);
            int server_ret = receiveData();
            if (server_ret != BLOCK_UPLOAD_FINISHED_RETURN_VAL) {
                throw new IOException("In upload content: upload packet failed ret =  " + server_ret);
            }
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
        if (server_ret != FILE_UPLOAD_FINISHED_RETURN_VAL) {
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
            mUploadCount += end - start;
            if (mListener != null) {
                float percent = (float) mUploadCount * 100 / totalCount;
                mListener.onUploadProgress(percent);
            }

            start += BLOCK_SIZE;
            if (eos) {
                break;
            }
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
        tran_data.block_sha1 = mPacketSHA1;
        tran_data.upload_type = ProtocolConstMsg.HISTORY_DATA;
        tran_data.block_num = block_num | (block_type << 30);
        tran_data.data_type = mDataType;
        tran_data.seq_num = packet_num;
        tran_data.length = (short) block_size;
        tran_data.buf = block;
        int ilen = CommWaylensParse.encode(comm_header, tran_data, data);
        sendData(data, ilen);
    }

    @NonNull
    private String getDeviceID() {
        return SessionManager.getInstance().getUserId() + "/" + mGuid;
    }
}
