package com.waylens.hachi.utils;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.transee.common.DateTime;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipUploadUrlRequest;
import com.waylens.hachi.snipe.toolbox.VdbImageRequest;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.vdb.UploadUrl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CountDownLatch;

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
    VdbRequestQueue mVdbRequestQueue;

    volatile boolean isCancelled;

    public DataUploaderV2(String address, int port, String privateKey) {
        mAddress = address;
        mPort = port;
        mPrivateKey = privateKey;
        mMomentID = 1014;
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

    private int createMomentDesc(SharableClip sharableClip, UploadUrl uploadUrl, int dataType) throws IOException {
        CrsMomentDescription momentDescription = new CrsMomentDescription(mUserId,
                mMomentID,
                "background music",
                mPrivateKey);
        momentDescription.addFragment(new CrsFragment(sharableClip.clip.getVdbId(),
                getClipCaptureTime(sharableClip, uploadUrl),
                uploadUrl.realTimeMs,
                0,
                uploadUrl.lengthMs,
                sharableClip.clip.streams[1].video_width,
                sharableClip.clip.streams[1].video_height,
                dataType
        ));
        momentDescription.addFragment(new CrsFragment(String.valueOf(mMomentID),
                getClipCaptureTime(sharableClip, uploadUrl),
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

    String getClipCaptureTime(SharableClip sharableClip, UploadUrl uploadUrl) {
        long offset = uploadUrl.realTimeMs - sharableClip.clip.getStartTimeMs();
        return DateTime.toString(sharableClip.clip.clipDate, offset);
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

    private int uploadMomentData(String guid, UploadUrl uploadUrl, int dataType) throws IOException {
        int ret = startUpload(guid, uploadUrl, dataType);
        if (ret != 0) {
            log("StartUpload Error/Already exist: " + ret);
            return ret;
        }

        InputStream inputStream = null;
        try {
            URL url = new URL(uploadUrl.url);
            URLConnection conn = url.openConnection();
            inputStream = conn.getInputStream();
            Log.e("test", String.format("ContentLength[%d]", conn.getContentLength()));
            return doUpload(guid, dataType, inputStream);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log("", e);
                }
            }
        }
    }

    private int doUpload(String guid, int dataType, InputStream inputStream) throws IOException {
        byte[] data = new byte[1024 * 4];
        int length;
        int seqNum = 0;
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
        }
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

    UploadUrl getFragmentData(SharableClip sharableClip, int dataType) {
        final CountDownLatch latch = new CountDownLatch(1);
        final UploadUrl[] urlWrapper = new UploadUrl[]{null};
        Bundle parameters = new Bundle();
        parameters.putBoolean(ClipUploadUrlRequest.PARAM_IS_PLAY_LIST, false);
        parameters.putLong(ClipUploadUrlRequest.PARAM_CLIP_TIME_MS, sharableClip.selectedStartValue);
        parameters.putInt(ClipUploadUrlRequest.PARAM_CLIP_LENGTH_MS, sharableClip.getSelectedLength());
        parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, dataType);

        ClipUploadUrlRequest request = new ClipUploadUrlRequest(sharableClip.bufferedCid, parameters,
                new VdbResponse.Listener<UploadUrl>() {
                    @Override
                    public void onResponse(UploadUrl response) {
                        urlWrapper[0] = response;
                        latch.countDown();
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        log("", error);
                        latch.countDown();
                    }
                });
        mVdbRequestQueue.add(request);
        try {
            latch.await();
        } catch (InterruptedException e) {
            log("", e);
        }
        return urlWrapper[0];
    }

    Bitmap getClipThumbnail(SharableClip sharableClip) {
        final CountDownLatch latch = new CountDownLatch(1);
        final Bitmap[] bitmaps = new Bitmap[]{null};
        VdbImageRequest request = new VdbImageRequest(
                sharableClip.getThumbnailClipPos(sharableClip.selectedStartValue),
                new VdbResponse.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        bitmaps[0] = response;
                        latch.countDown();
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        log("", error);
                        latch.countDown();
                    }
                },
                0, 0, ImageView.ScaleType.CENTER_INSIDE, Bitmap.Config.RGB_565, null
        );
        mVdbRequestQueue.add(request);
        try {
            latch.await();
        } catch (InterruptedException e) {
            log("", e);
        }
        return bitmaps[0];
    }

    private void logOut() throws IOException {
        CrsUserLogout logout = new CrsUserLogout(mUserId, mPrivateKey);
        sendData(logout.getEncodedCommand());
    }

    private int uploadThumbnail(SharableClip sharableClip) throws IOException {
        int ret = startUpload(String.valueOf(mMomentID), null, CrsCommand.VIDIT_THUMBNAIL_JPG);
        if (ret == CrsCommand.RES_FILE_TRANS_COMPLETE) {
            log("Already exists. uploadThumbnail successful");
            return ret;
        }
        if (ret != 0) {
            log("Start uploadThumbnail error: " + ret);
            return ret;
        }

        Bitmap bitmap = getClipThumbnail(sharableClip);
        ByteArrayOutputStream bitmapOut = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bitmapOut);
        byte[] bytes = bitmapOut.toByteArray();
        log("Thumbnail size: " + bytes.length);
        ByteArrayInputStream bitmapIn = new ByteArrayInputStream(bytes);
        return doUpload(String.valueOf(mMomentID), CrsCommand.VIDIT_THUMBNAIL_JPG, bitmapIn);

    }

    public int upload(VdbRequestQueue vdbRequestQueue, long momentID, SharableClip sharableClip, int dataType) {
        mVdbRequestQueue = vdbRequestQueue;
        int vdbDataType = 0;
        if ((dataType & CrsCommand.VIDIT_VIDEO_DATA_LOW) == CrsCommand.VIDIT_VIDEO_DATA_LOW) {
            vdbDataType = VdbCommand.Factory.UPLOAD_GET_V1;
        }
        if ((dataType & CrsCommand.VIDIT_RAW_DATA) == CrsCommand.VIDIT_RAW_DATA) {
            vdbDataType |= VdbCommand.Factory.UPLOAD_GET_RAW;
        }

        UploadUrl uploadUrl = getFragmentData(sharableClip, vdbDataType);

        mMomentID = momentID;
        try {
            init();
            int ret = login();
            if (ret != 0) {
                log("Login error");
                return ret;
            }
            log("Login successful");
            ret = createMomentDesc(sharableClip, uploadUrl, dataType);
            if (ret != 0) {
                log("createMoment error");
                return ret;
            }
            log("createMoment successful");

            ret = uploadMomentData(sharableClip.clip.getVdbId(), uploadUrl, dataType);
            if (ret != CrsCommand.RES_FILE_TRANS_COMPLETE) {
                log("Upload fragment error: " + ret);
                return ret;
            }
            log("Upload fragment successful");
            ret = uploadThumbnail(sharableClip);
            if (ret != CrsCommand.RES_FILE_TRANS_COMPLETE) {
                log("Upload thumbnail error: " + ret);
                return ret;
            }
            log("Upload thumbnail successful");
            return CrsCommand.RES_STATE_OK;
        } catch (IOException e) {
            log("", e);
            if (isCancelled) {
                return CrsCommand.RES_STATE_CANCELLED;
            } else {
                return CrsCommand.RES_STATE_FAIL;
            }
        } finally {
            try {
                logOut();
            } catch (Exception e) {
                log("", e);
            }
        }
    }

    public void cancel() {
        log("Cancel task....");
        isCancelled = true;
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                log("Close OutputStream", e);
            }
        }
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                log("Close Socket", e);
            }
        }
    }

    void log(String msg, Exception e) {
        Log.e(TAG, msg, e);
    }

    void log(String msg) {
        Log.e(TAG, msg);
    }
}
