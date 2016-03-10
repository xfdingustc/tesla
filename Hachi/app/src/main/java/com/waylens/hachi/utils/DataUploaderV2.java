package com.waylens.hachi.utils;

import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.ui.helpers.MomentShareHelper;
import com.waylens.hachi.vdb.urls.UploadUrl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

import crs_svr.v2.CrsClientTranData;
import crs_svr.v2.CrsCommand;
import crs_svr.v2.CrsCommandResponse;
import crs_svr.v2.CrsFragment;
import crs_svr.v2.CrsMomentDescription;
import crs_svr.v2.CrsUserLogin;
import crs_svr.v2.CrsUserLogout;
import crs_svr.v2.CrsUserStartUpload;
import crs_svr.v2.CrsUserStopUpload;
import crs_svr.v2.EncodeCommandHeader;

/**
 * DataUploaderV2
 * Created by Richard on 1/14/16.
 */
public class DataUploaderV2 {
    private static final String TAG = "DataUploaderV2";

    private String mAddress;
    private int mPort;
    private Socket mSocket;
    private OutputStream mOutputStream;
    private String mPrivateKey;
    String mUserId;
    long mMomentID;

    volatile boolean isCancelled;
    private int mClipTotalCount;

    private OnUploadListener mUploadListener;

    public interface OnUploadListener {
        void onUploadSuccessful();

        void onUploadProgress(int percentage);

        void onUploadError(int errorCode, int extraCode);

        void onCancelUpload();
    }

    public DataUploaderV2() {
        mUserId = SessionManager.getInstance().getUserId();
        EncodeCommandHeader.CURRENT_ENCODE_TYPE = CrsCommand.ENCODE_TYPE_OPEN;
    }

    private void init() throws IOException {
        mSocket = new Socket(mAddress, mPort);
        mOutputStream = mSocket.getOutputStream();
    }

    private int login() throws IOException {
        CrsUserLogin loginCmd = new CrsUserLogin(mUserId, mMomentID, mPrivateKey);
        sendData(loginCmd.getEncodedCommand());
        return receiveData();
    }

    private int createMomentDesc(LocalMoment localMoment)
            throws IOException {
        CrsMomentDescription momentDescription = new CrsMomentDescription(mUserId,
                mMomentID,
                "background music",
                mPrivateKey);
        for (LocalMoment.Fragment fragment : localMoment.fragments) {
            momentDescription.addFragment(new CrsFragment(fragment.clip.getVdbId(),
                    fragment.getClipCaptureTime(),
                    fragment.uploadURL.realTimeMs,
                    0,
                    fragment.uploadURL.lengthMs,
                    fragment.clip.streams[1].video_width,
                    fragment.clip.streams[1].video_height,
                    fragment.dataType
            ));
        }
        momentDescription.addFragment(new CrsFragment(String.valueOf(mMomentID),
                localMoment.fragments.get(0).getClipCaptureTime(),
                0,
                0,
                0,
                (short) 0,
                (short) 0,
                CrsCommand.VIDIT_THUMBNAIL_JPG
        ));
        sendData(momentDescription.getEncodedCommand());
        return receiveData();
    }

    private int startUpload(String guid, UploadUrl uploadUrl, int dataType) throws IOException {
        long startTime = 0;
        long offset = 0;
        int duration = 0;
        if (uploadUrl != null) {
            startTime = uploadUrl.realTimeMs;
            duration = uploadUrl.lengthMs;
        }
        CrsUserStartUpload startUpload = new CrsUserStartUpload(
                mUserId,
                guid,
                mMomentID,
                null,
                dataType,
                0,
                startTime,
                offset,
                duration,
                mPrivateKey

        );
        sendData(startUpload.getEncodedCommand());
        return receiveData();
    }

    private int uploadMomentData(LocalMoment localMoment) throws IOException {
        mClipTotalCount = localMoment.fragments.size() + 1; // 1 for thumbnail
        int clipIndex = 0;
        for (LocalMoment.Fragment fragment : localMoment.fragments) {
            String guid = fragment.clip.getVdbId();
            UploadUrl uploadUrl = fragment.uploadURL;
            int ret = startUpload(guid, uploadUrl, fragment.dataType);
            if (ret == CrsCommand.RES_FILE_TRANS_COMPLETE) {
                Logger.t(TAG).d("File already exist: " + ret);
                continue;
            }

            if (ret != CrsCommand.RES_STATE_OK) {
                Logger.t(TAG).d("Start upload: " + ret);
                return ret;
            }

            InputStream inputStream = null;
            try {
                URL url = new URL(uploadUrl.url);
                URLConnection conn = url.openConnection();
                inputStream = conn.getInputStream();
                Logger.t(TAG).d("test", String.format("ContentLength[%d]", conn.getContentLength()));
                ret = doUpload(guid, conn.getContentLength(), fragment.dataType, inputStream, clipIndex++);
                if (ret != CrsCommand.RES_FILE_TRANS_COMPLETE) {
                    return ret;
                }
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Logger.t(TAG).e(e, "Close inputStream");
                    }
                }
            }
        }
        return CrsCommand.RES_FILE_TRANS_COMPLETE;
    }

    private int doUpload(String guid, int totalLength, int dataType, InputStream inputStream, int clipIndex)
            throws IOException {
        byte[] data = new byte[1024 * 4];
        int length;
        int seqNum = 0;
        int dataSend = 0;
        while ((length = inputStream.read(data)) != -1) {
            if (length < data.length) {
                int secondLength = inputStream.read(data, length, data.length - length);
                if (secondLength > 0) {
                    length += secondLength;
                }
            }

            CrsClientTranData tranData = new CrsClientTranData(mUserId,
                    guid,
                    mMomentID,
                    null,
                    null,
                    dataType,
                    seqNum,
                    0,
                    (short) length,
                    data,
                    mPrivateKey);
            sendData(tranData.getEncodedCommand());
            seqNum++;
            dataSend += length;

            int percentageInThisClip = dataSend * 100 / totalLength / mClipTotalCount;

            int percentage = clipIndex * 100 / mClipTotalCount + percentageInThisClip;

            mUploadListener.onUploadProgress(percentage);
        }

        inputStream.close();

        return stopUpload(guid);
    }

    private int stopUpload(String guid) throws IOException {
        CrsUserStopUpload crsUserStopUpload = new CrsUserStopUpload(mUserId, guid, mPrivateKey);
        sendData(crsUserStopUpload.getEncodedCommand());
        return receiveData();
    }

    private void sendData(byte[] buffer) throws IOException {
        sendData(buffer, buffer.length);
    }

    private void sendData(byte[] buffer, int bufferSize) throws IOException {
        mOutputStream.write(buffer, 0, bufferSize);
        mOutputStream.flush();
    }

    private int receiveData() throws IOException {
        byte[] data = new byte[128];
        InputStream input = mSocket.getInputStream();
        int length = input.read(data);
        if (length != -1) {
            CrsCommandResponse response = (CrsCommandResponse) new CrsCommandResponse().decodeCommand(data);
            return response.responseCode;
        } else {
            return CrsCommandResponse.RES_STATE_FAIL;
        }
    }

    private void logOut() throws IOException {
        CrsUserLogout logout = new CrsUserLogout(mUserId, mPrivateKey);
        sendData(logout.getEncodedCommand());
    }

    private int uploadThumbnail(String thumbnailPath) throws IOException {
        int ret = startUpload(String.valueOf(mMomentID), null, CrsCommand.VIDIT_THUMBNAIL_JPG);
        if (ret == CrsCommand.RES_FILE_TRANS_COMPLETE) {
            Logger.t(TAG).d("Already exists. uploadThumbnail successful");
            return ret;
        }

        if (ret != 0) {
            Logger.t(TAG).d("Start uploadThumbnail error: " + ret);
            return ret;
        }

        FileInputStream fis = new FileInputStream(thumbnailPath);
        return doUpload(String.valueOf(mMomentID), fis.available(), CrsCommand.VIDIT_THUMBNAIL_JPG,
                fis, mClipTotalCount - 1);

    }

    void uploadClips(LocalMoment localMoment) {
        mMomentID = localMoment.momentID;
        Logger.t(TAG).e("MomentID: " + mMomentID);
        try {
            init();
            int ret = login();
            if (ret != 0) {
                Logger.t(TAG).d("Login error");
                mUploadListener.onUploadError(MomentShareHelper.ERROR_LOGIN, ret);
                return;
            }
            Logger.t(TAG).d("Login successful");
            ret = createMomentDesc(localMoment);
            if (ret != 0) {
                Logger.t(TAG).d("createMoment error");
                mUploadListener.onUploadError(MomentShareHelper.ERROR_CREATE_MOMENT_DESC, ret);
                return;
            }
            Logger.t(TAG).d("createMoment successful");

            ret = uploadMomentData(localMoment);
            if (ret != CrsCommand.RES_FILE_TRANS_COMPLETE) {
                Logger.t(TAG).d("Upload fragment error: " + ret);
                mUploadListener.onUploadError(MomentShareHelper.ERROR_UPLOAD_VIDEO, ret);
                return;
            }
            Logger.t(TAG).d("Upload fragment successful");
            ret = uploadThumbnail(localMoment.thumbnailPath);
            if (ret != CrsCommand.RES_FILE_TRANS_COMPLETE) {
                Logger.t(TAG).d("Upload thumbnail error: " + ret);
                mUploadListener.onUploadError(MomentShareHelper.ERROR_UPLOAD_THUMBNAIL, ret);
                return;
            }
            Logger.t(TAG).d("Upload thumbnail successful");
            mUploadListener.onUploadSuccessful();
        } catch (IOException e) {
            Logger.t(TAG).e(e, "IOException");
            if (isCancelled) {
                mUploadListener.onCancelUpload();
            } else {
                mUploadListener.onUploadError(MomentShareHelper.ERROR_IO, CrsCommand.RES_STATE_FAIL);
            }
        } finally {
            try {
                logOut();
            } catch (Exception e) {
                Logger.t(TAG).e(e, "Exception");
            }
        }

    }

    public void upload(LocalMoment localMoment, @NonNull OnUploadListener listener) {
        mAddress = localMoment.cloudInfo.address;
        mPort = localMoment.cloudInfo.port;
        mPrivateKey = localMoment.cloudInfo.privateKey;
        mUploadListener = listener;
        uploadClips(localMoment);

    }

    public void cancel() {
        Logger.t(TAG).d("Cancel task....");

        isCancelled = true;
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                Logger.t(TAG).d("Close OutputStream");
            }
        }
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                Logger.t(TAG).d("Close Socket");
            }
        }
    }
}
