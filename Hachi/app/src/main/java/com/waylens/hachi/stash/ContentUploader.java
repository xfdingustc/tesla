package com.waylens.hachi.stash;

import android.util.Log;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.utils.DigitUtils;
import com.waylens.hachi.utils.HashUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import crs_svr.CommWaylensParse;
import crs_svr.CrsClientLogin;
import crs_svr.CrsClientStartUpload;
import crs_svr.CrsClientTranData;
import crs_svr.CrsCommResponse;
import crs_svr.ProtocolCommand;
import crs_svr.ProtocolConstMsg;
import crs_svr.WaylensCommHead;

/**
 * Created by Xiaofei on 2015/3/18.
 */
public class ContentUploader {
    private static final String TAG = ContentUploader.class.getSimpleName();

    private static final String DEFAULT_CONTENT_TOKEN = "avatar";

    private String mIpAddr;
    private int mPort;
    private String mPrivKey;

    private UploadListener mListener;

    private File mFile;

    public ContentUploader(JSONObject token, File file) {
        parseToken(token);
        mFile = file;
        CommWaylensParse.private_key = mPrivKey;
        CommWaylensParse.encode_type = (byte) ProtocolConstMsg.ENCODE_TYPE_OPEN;
    }

    private byte getDataType() {
        return ProtocolConstMsg.JPEG_AVATAR;
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


    private void parseToken(JSONObject token) {
        try {
            JSONObject uploadServer = token.getJSONObject("uploadServer");
            mIpAddr = uploadServer.optString("ip");
            mPort = uploadServer.optInt("port");
            mPrivKey = uploadServer.optString("privateKey");
        } catch (Exception e) {
            Logger.t(TAG).e(e, "");
        }
    }

    public boolean upload() {
        if (mFile == null) {
            Log.i(TAG, "File does not exist.");
            return false;
        }
        FileUploader fileUploader = new FileUploader(mFile, getDataType());
        fileUploader.upload();
        return true;

    }

    private void notifyUploadFinished() {
        if (mListener != null) {
            mListener.onUploadFinished();
        }
    }

    private class FileUploader {
        private final int BLOCK_UPLOAD_FINISHED_RETURN_VAL = 1;
        private final int FILE_UPLOAD_FINISHED_RETURN_VAL = 2;
        private File mFile;
        private byte mDataType;

        private byte[] mFileSha1;
        private byte[] mPacketSha1;

        private int mUploadCnt = 0;


        private OutputStream mOutputStream;
        private Socket mSocket;

        public FileUploader(File file, byte dataType) {
            this.mFile = file;
            this.mDataType = dataType;
            init();
        }

        private void init() {
            mFileSha1 = HashUtils.SHA1(mFile);

            try {
                mSocket = new Socket(mIpAddr, mPort);
                mOutputStream = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public boolean upload() {
            try {
                login();
                Logger.t(TAG).d("Login finished!!!");
                if (!startUpload()) {
                    Log.i(TAG, "This file already exists");
                    return true;
                } else {
                    Log.i(TAG, "Start upload finished");
                }
                doUpload();
                Log.i(TAG, "do upload finished!!!");
                notifyUploadFinished();
                stopUpload();
                release();
                Log.i(TAG, "release finished!!!!");
                return true;
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        private void doUpload() throws IOException, IllegalStateException {
            byte[] data = new byte[4 * 1024 * 1024];
            int packet_num = 0;

            InputStream input_stream = new FileInputStream(mFile);
            while (true) {
                int read_bytes_cnt = input_stream.read(data);

                if (read_bytes_cnt < 0) {
                    break;
                }
                mPacketSha1 = HashUtils.SHA1(data, read_bytes_cnt);
                uploadPacket(data, read_bytes_cnt, packet_num++);
                int server_ret = receiveData();
                if (server_ret != BLOCK_UPLOAD_FINISHED_RETURN_VAL) {
                    throw new IOException("In upload content: upload packet failed ret =  " + server_ret);
                }
            }

            int server_ret = receiveData();
            if (server_ret != FILE_UPLOAD_FINISHED_RETURN_VAL) {
                throw new IOException("In upload content: upload file failed ret = " + server_ret);
            }

        }

        private void login() throws IOException {
            byte[] data = new byte[512];
            WaylensCommHead comm_header = new WaylensCommHead();
            comm_header.version = ProtocolConstMsg.WAYLENS_VERSION;
            comm_header.cmd = ProtocolCommand.CRS_C2S_LOGIN;
            comm_header.size = WaylensCommHead.COMM_HEAD_LEGTH;

            CrsClientLogin login = new CrsClientLogin();
            login.device_id = SessionManager.getInstance().getUserId() + "/android";
            login.token = DEFAULT_CONTENT_TOKEN;

            String hash = login.device_id + mPrivKey;
            login.hash_value = HashUtils.MD5String(hash);
            login.device_type = ProtocolConstMsg.DEVICE_OTHER;

            int ilen = CommWaylensParse.encode(comm_header, login, data);

            sendData(data, ilen);
            int ret = receiveData();
            if (ret != 0) {
                throw new RuntimeException("In upload content: login failed ret = " + ret);
            }
        }

        private boolean startUpload() throws IOException {
            byte[] data = new byte[512];

            WaylensCommHead comm_header = new WaylensCommHead();
            comm_header.version = ProtocolConstMsg.WAYLENS_VERSION;
            comm_header.cmd = ProtocolCommand.CRS_C2S_START_UPLOAD;
            comm_header.size = WaylensCommHead.COMM_HEAD_LEGTH;

            CrsClientStartUpload start_upload = new CrsClientStartUpload();
            start_upload.device_id = SessionManager.getInstance().getUserId() + "/android";
            start_upload.token = DEFAULT_CONTENT_TOKEN;
            start_upload.file_sha1 = mFileSha1;
            start_upload.upload_type = ProtocolConstMsg.HISTORY_DATA;
            start_upload.data_type = mDataType;
            start_upload.file_size = mFile.length();

            int ilen = CommWaylensParse.encode(comm_header, start_upload, data);

            sendData(data, ilen);
            Logger.t(TAG).d("sha1: " + new String(mFileSha1) + " sha1 size: " + mFileSha1.length);
            int ret = receiveData();


            // File already exist, so return false;
            if (ret == 2) {
                return false;
            }

            if (ret != 0) {
                //throw new RuntimeException("In upload content: start upload failed ret = " + ret);
                return false;
            }

            if (mListener != null) {
                mListener.onUploadStarted();
            }
            return true;
        }

        public void stopUpload() {

        }

        private void sendData(byte[] buffer, int buffer_size) throws IOException {
            mOutputStream.write(buffer, 0, buffer_size);
            mOutputStream.flush();
        }

        private void uploadPacket(byte[] packet, int packet_size, int packet_num) throws IOException,
                IllegalStateException {
            int start, end;
            start = 0;
            boolean eos = false;
            boolean first_block = true;
            int block_type;
            int block_num = 0;

            while (true) {
                end = start + 1024;
                if (end >= packet_size) {
                    end = packet_size;
                    eos = true;
                }

                if (first_block) {
                    first_block = false;
                    block_type = 0b01;
                    if (eos && packet_num == 0) {
                        block_type = 0b00;
                    }
                } else {
                    if (eos) {
                        block_type = 0b11;
                    } else {
                        block_type = 0b10;
                    }
                }

                byte[] data = Arrays.copyOfRange(packet, start, end);
                uploadBlock(data, end - start, block_type, block_num++, packet_num);
                mUploadCnt += end - start;
                if (mListener != null) {
                    float percent = (float) mUploadCnt * 100 / mFile.length();
                    mListener.onUploadProgress(percent);
                }

                start += 1024;
                if (eos) {
                    break;
                }
            }
        }

        private void uploadBlock(byte[] block, int block_size, int block_type, int block_num,
                                 int packet_num) throws IOException {

            byte[] data = new byte[8 * 1024];

            WaylensCommHead comm_header = new WaylensCommHead();
            comm_header.version = ProtocolConstMsg.WAYLENS_VERSION;
            comm_header.cmd = ProtocolCommand.CRS_UPLOADING_DATA;
            comm_header.size = WaylensCommHead.COMM_HEAD_LEGTH;

            CrsClientTranData tran_data = new CrsClientTranData();
            tran_data.device_id = SessionManager.getInstance().getUserId() + "/android";
            tran_data.token = DEFAULT_CONTENT_TOKEN;
            tran_data.file_sha1 = mFileSha1;
            tran_data.block_sha1 = mPacketSha1;
            tran_data.upload_type = ProtocolConstMsg.HISTORY_DATA;
            tran_data.block_num = block_num | (block_type << 30);
            tran_data.data_type = mDataType;
            tran_data.seq_num = packet_num;
            tran_data.length = (short) block_size;
            tran_data.buf = block;
            int ilen = CommWaylensParse.encode(comm_header, tran_data, data);
            sendData(data, ilen);
        }

        private int receiveData() throws IOException {
            byte[] data = new byte[8192];
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

            Logger.t(TAG).d("return = " + response.ret + " comm type = " + comm_head.cmd);
            return response.ret;
        }

        private void release() throws IOException {
            mOutputStream.close();
            mSocket.close();
        }
    }


}
