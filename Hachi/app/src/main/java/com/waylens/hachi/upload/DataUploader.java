package com.waylens.hachi.upload;

import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.ui.helpers.MomentShareHelper;
import com.waylens.hachi.upload.event.UploadEvent;
import com.waylens.hachi.utils.HashUtils;
import com.waylens.hachi.vdb.urls.UploadUrl;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
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
public class DataUploader {
    private static final String TAG = DataUploader.class.getSimpleName();

    private CloudInfo mCloudInfo;
    private Socket mSocket;
    private OutputStream mOutputStream;

    String mUserId;
    long mMomentID;

    private EventBus mEventBus = EventBus.getDefault();

    volatile boolean isCancelled;
    private int mClipTotalCount;


    public DataUploader() {
        mUserId = SessionManager.getInstance().getUserId();
        EncodeCommandHeader.CURRENT_ENCODE_TYPE = CrsCommand.ENCODE_TYPE_OPEN;
    }

    private void init() throws IOException {
        mSocket = new Socket(mCloudInfo.getAddress(), mCloudInfo.getPort());
        mOutputStream = mSocket.getOutputStream();
    }

    private int login() throws IOException {
        mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_LOGIN));
        CrsUserLogin loginCmd = new CrsUserLogin(mUserId, mMomentID, mCloudInfo.getPrivateKey());
        sendData(loginCmd.getEncodedCommand());
        int ret = receiveData();
        mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_LOGIN_SUCCEED));
        return ret;
    }

    private int createMomentDesc(LocalMoment localMoment)
        throws IOException {
        CrsMomentDescription momentDescription = new CrsMomentDescription(mUserId, mMomentID, "background music", mCloudInfo.getPrivateKey());
        for (LocalMoment.Segment segment : localMoment.mSegments) {
            momentDescription.addFragment(new CrsFragment(segment.clip.getVdbId(),
                segment.getClipCaptureTime(),
                segment.uploadURL.realTimeMs,
                0,
                segment.uploadURL.lengthMs,
                segment.clip.streams[1].video_width,
                segment.clip.streams[1].video_height,
                segment.dataType
            ));
        }
        CrsFragment  crsFragment = new CrsFragment(String.valueOf(mMomentID),
            localMoment.mSegments.get(0).getClipCaptureTime(), 0, 0, 0,
            (short)0, (short) 0, CrsCommand.VIDIT_THUMBNAIL_JPG);
        momentDescription.addFragment(crsFragment);
        sendData(momentDescription.getEncodedCommand());
        return receiveData();
    }

    private int startUpload(String guid, UploadUrl uploadUrl, int dataType, int fileSize, byte[] fileSha1) throws IOException {
        long startTime = 0;
        long offset = 0;
        int duration = 0;
        if (uploadUrl != null) {
            startTime = uploadUrl.realTimeMs;
            duration = uploadUrl.lengthMs;
        }
        CrsUserStartUpload startUpload = new CrsUserStartUpload(mUserId, guid, mMomentID, fileSha1,
            dataType, fileSize, startTime, offset, duration, mCloudInfo.getPrivateKey());
        sendData(startUpload.getEncodedCommand());
        return receiveData();
    }

    private int uploadMomentData(LocalMoment localMoment) throws IOException {
        mClipTotalCount = localMoment.mSegments.size() + 1; // 1 for thumbnail
        int clipIndex = 0;
        for (LocalMoment.Segment segment : localMoment.mSegments) {
            String guid = segment.clip.getVdbId();
            UploadUrl uploadUrl = segment.uploadURL;
            int ret = startUpload(guid, uploadUrl, segment.dataType, 0, null);
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
                ret = doUpload(guid, conn.getContentLength(), segment.dataType, inputStream, clipIndex++, null);
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

    private int doUpload(String guid, int totalLength, int dataType, InputStream inputStream,
                         int clipIndex, byte[] fileSha1) throws IOException {
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

            byte[] blockSha1 = HashUtils.SHA1(data, length);

//            Logger.t(TAG).d("upload one block " + length + " total length: " + totalLength);

            CrsClientTranData tranData = new CrsClientTranData(mUserId, guid, mMomentID, fileSha1,
                blockSha1, dataType, seqNum, 0, (short) length, data, mCloudInfo.getPrivateKey());

            sendData(tranData.getEncodedCommand());
            seqNum++;
            dataSend += length;

            int percentageInThisClip = dataSend * 100 / totalLength / mClipTotalCount;

            int percentage = clipIndex * 100 / mClipTotalCount + percentageInThisClip;

//            mUploadListener.onUploadProgress(percentage);
            Logger.t(TAG).d("upload progress: " + percentage);
            mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_PROGRESS, percentage));

        }

        inputStream.close();
        int serverRet = receiveData();
        if (serverRet != CrsCommand.RES_FILE_TRANS_COMPLETE) {
            throw new IOException("In upload content: upload file failed ret = " + serverRet);
        }


        return stopUpload(guid);
    }

    private int stopUpload(String guid) throws IOException {
        CrsUserStopUpload crsUserStopUpload = new CrsUserStopUpload(mUserId, guid, mCloudInfo.getPrivateKey());
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
        CrsUserLogout logout = new CrsUserLogout(mUserId, mCloudInfo.getPrivateKey());
        sendData(logout.getEncodedCommand());
    }

    private int uploadThumbnail(String thumbnailPath) throws IOException {
        int ret = startUpload(String.valueOf(mMomentID), null, CrsCommand.VIDIT_THUMBNAIL_JPG, 0, null);
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
            fis, mClipTotalCount - 1, null);

    }

    private int uploadAvatar(String thumbnailPath, int fileSize, byte[] fileSha1) throws IOException {
        int ret = startUpload(String.valueOf(mMomentID), null, CrsCommand.JPEG_AVATAR, fileSize, fileSha1);
        if (ret == CrsCommand.RES_FILE_TRANS_COMPLETE) {
            Logger.t(TAG).d("Already exists. uploadThumbnail successful");
            return ret;
        }

        if (ret != 0) {
            Logger.t(TAG).d("Start uploadThumbnail error: " + ret);
            return ret;
        }

        FileInputStream fis = new FileInputStream(thumbnailPath);
        mClipTotalCount = 1;
        return doUpload(String.valueOf(mMomentID), fis.available(), CrsCommand.JPEG_AVATAR,
            fis, mClipTotalCount - 1, fileSha1);

    }


    void uploadClips(LocalMoment localMoment) {
        mMomentID = localMoment.momentID;
        Logger.t(TAG).e("MomentID: " + mMomentID);
        try {
            init();
            int ret = login();
            if (ret != 0) {
                Logger.t(TAG).d("Login error");
                mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_ERROR, UploadEvent.UPLOAD_ERROR_LOGIN));
                return;
            }
            Logger.t(TAG).d("Login successful");
            ret = createMomentDesc(localMoment);
            if (ret != 0) {
                Logger.t(TAG).d("createMoment error");
                mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_ERROR, UploadEvent.UPLOAD_ERROR_CREATE_MOMENT_DESC));
                return;
            }
            Logger.t(TAG).d("createMoment successful");

            ret = uploadMomentData(localMoment);
            if (ret != CrsCommand.RES_FILE_TRANS_COMPLETE) {
                Logger.t(TAG).d("Upload fragment error: " + ret);
                mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_ERROR, UploadEvent.UPLOAD_ERROR_UPLOAD_VIDEO));
                return;
            }
            Logger.t(TAG).d("Upload fragment successful");
            ret = uploadThumbnail(localMoment.thumbnailPath);
            if (ret != CrsCommand.RES_FILE_TRANS_COMPLETE) {
                Logger.t(TAG).d("Upload thumbnail error: " + ret);
                mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_ERROR, UploadEvent.UPLOAD_ERROR_UPLOAD_THUMBNAIL));
                return;
            }
            Logger.t(TAG).d("Upload thumbnail successful");
            mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_FINISHED));

        } catch (IOException e) {
            Logger.t(TAG).e(e, "IOException");
            if (isCancelled) {

                mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_CANCELLED));
            } else {
                mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_ERROR, UploadEvent.UPLOAD_ERROR_IO));
//                mUploadListener.onUploadError(MomentShareHelper.ERROR_IO, CrsCommand.RES_STATE_FAIL);
            }
        } finally {
            try {
                logOut();
            } catch (Exception e) {
                Logger.t(TAG).e(e, "Exception");
            }
        }

    }

    void updateFile(String file) {
        Logger.t(TAG).d("Start upload file: " + file.toString());
        try {
            init();
            int ret = login();
            if (ret != 0) {
                Logger.t(TAG).d("Login error");
//                mUploadListener.onUploadError(MomentShareHelper.ERROR_LOGIN, ret);
                mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_ERROR, UploadEvent.UPLOAD_ERROR_LOGIN));
                return;
            }
            Logger.t(TAG).d("Login successful");
            File avatarFile = new File(file);

            byte[] fileSha1 = HashUtils.SHA1(avatarFile);
            int fileSize = (int) avatarFile.length();

            ret = uploadAvatar(file, fileSize, fileSha1);
            if (ret != CrsCommand.RES_STATE_OK) {
                Logger.t(TAG).d("Upload thumbnail error: " + ret);
                mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_ERROR, UploadEvent.UPLOAD_ERROR_UPLOAD_THUMBNAIL));
                return;
            }
            Logger.t(TAG).d("Upload thumbnail successful");
            mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_FINISHED));
        } catch (IOException e) {
            e.printStackTrace();
            if (isCancelled) {
                mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_CANCELLED));
            } else {
                mEventBus.post(new UploadEvent(UploadEvent.UPLOAD_WHAT_ERROR, UploadEvent.UPLOAD_ERROR_IO));
            }
        } finally {
            try {
                logOut();
            } catch (Exception e) {
                Logger.t(TAG).e(e, "Exception");
            }
        }
    }

    public void upload(LocalMoment localMoment) {
        mCloudInfo = localMoment.cloudInfo;

        uploadClips(localMoment);

    }

    public void upload(CloudInfo cloudInfo, String file) {
        mCloudInfo = cloudInfo;
        updateFile(file);
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
