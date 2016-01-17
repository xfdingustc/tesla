package com.waylens.hachi.utils;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipUploadUrlRequest;
import com.waylens.hachi.snipe.toolbox.VdbImageRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
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
import crs_svr.v2.CrsUserExitRequest;
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
    //Clip mClip;
    VdbRequestQueue mVdbRequestQueue;

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

    private int createMomentDesc(Clip clip, UploadUrl uploadUrl, int dataType) throws IOException {
        CrsMomentDescription momentDescription = new CrsMomentDescription(mUserId,
                mMomentID,
                "background music",
                mPrivateKey);
        momentDescription.addFragment(new CrsFragment(clip.getVdbId(),
                clip.getDateTimeString(),
                uploadUrl.realTimeMs,
                0,
                uploadUrl.lengthMs,
                clip.streams[1].video_width,
                clip.streams[1].video_height,
                dataType
        ));
        momentDescription.addFragment(new CrsFragment(String.valueOf(mMomentID),
                clip.getDateTimeString(),
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

    private int uploadMomentData(Clip clip, UploadUrl uploadUrl, int dataType) {
        InputStream inputStream = null;
        try {
            URL url = new URL(uploadUrl.url);
            URLConnection conn = url.openConnection();
            inputStream = conn.getInputStream();
            Log.e("test", String.format("ContentLength[%d]", conn.getContentLength()));
            return doUpload(clip.getVdbId(), dataType, inputStream);
        } catch (Exception e) {
            Log.e(TAG, "", e);
            return -1;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
            }
        }
    }

    private int doUpload(String guid, int dataType, InputStream inputStream) throws IOException {
        byte[] data = new byte[1024 * 4];
        int length;
        int seqNum = 0;
        while ((length = inputStream.read(data)) != -1) {
            int secondLength = 0;
            if (length < data.length) {
                secondLength = inputStream.read(data, length, data.length - length);
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

    UploadUrl getFragmentData(Clip clip, int dataType) {
        final CountDownLatch latch = new CountDownLatch(1);
        final UploadUrl[] urlWrapper = new UploadUrl[]{null};
        Bundle parameters = new Bundle();
        parameters.putBoolean(ClipUploadUrlRequest.PARAM_IS_PLAY_LIST, false);
        parameters.putLong(ClipUploadUrlRequest.PARAM_CLIP_TIME_MS, clip.getStartTimeMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_CLIP_LENGTH_MS, clip.getDurationMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, dataType);

        ClipUploadUrlRequest request = new ClipUploadUrlRequest(clip, parameters,
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
                        Log.e(TAG, "", error);
                        latch.countDown();
                    }
                });
        mVdbRequestQueue.add(request);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "", e);
        }
        return urlWrapper[0];
    }

    Bitmap getClipThumbnail(Clip clip) {
        final CountDownLatch latch = new CountDownLatch(1);
        final Bitmap[] bitmaps = new Bitmap[]{null};
        VdbImageRequest request = new VdbImageRequest(
                new ClipPos(clip.getVdbId(), clip.cid, clip.clipDate, 0, ClipPos.TYPE_POSTER, false),
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
                        Log.e(TAG, "", error);
                        latch.countDown();
                    }
                },
                0, 0, ImageView.ScaleType.CENTER_INSIDE, Bitmap.Config.RGB_565, null
        );
        mVdbRequestQueue.add(request);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "", e);
        }
        return bitmaps[0];
    }

    private void logOut() throws IOException {
        CrsUserLogout logout = new CrsUserLogout(mUserId, mPrivateKey);
        sendData(logout.getEncodedCommand());
    }

    private int uploadThumbnail(Clip clip) throws IOException {
        int ret = startUpload(String.valueOf(mMomentID), null, CrsCommand.VIDIT_THUMBNAIL_JPG);
        if (ret == CrsCommand.RES_FILE_TRANS_COMPLETE) {
            Log.e(TAG, "Already exists. uploadThumbnail successful");
            return ret;
        }
        if (ret != 0) {
            Log.e(TAG, "Start uploadThumbnail error: " + ret);
            return ret;
        }

        Bitmap bitmap = getClipThumbnail(clip);
        ByteArrayOutputStream bitmapOut = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bitmapOut);
        byte[] bytes = bitmapOut.toByteArray();
        Log.e(TAG, "Thumbnail size: " + bytes.length);
        ByteArrayInputStream bitmapIn = new ByteArrayInputStream(bytes);
        return doUpload(String.valueOf(mMomentID), CrsCommand.VIDIT_THUMBNAIL_JPG, bitmapIn);

    }

    public void test(VdbRequestQueue vdbRequestQueue, long momentID, Clip clip, int dataType) {
        mVdbRequestQueue = vdbRequestQueue;
        int vdbDataType = 0;
        if ((dataType & CrsCommand.VIDIT_VIDEO_DATA_LOW) == CrsCommand.VIDIT_VIDEO_DATA_LOW) {
            vdbDataType = VdbCommand.Factory.UPLOAD_GET_V1;
        }
        if ((dataType & CrsCommand.VIDIT_RAW_DATA) == CrsCommand.VIDIT_RAW_DATA) {
            vdbDataType |= VdbCommand.Factory.UPLOAD_GET_RAW;
        }

        UploadUrl uploadUrl = getFragmentData(clip, vdbDataType);

        mMomentID = momentID;
        try {
            init();
            int ret = login();
            if (ret != 0) {
                Log.e(TAG, "Login error");
                return;
            }
            Log.e(TAG, "Login successful");
            ret = createMomentDesc(clip, uploadUrl, dataType);
            if (ret != 0) {
                Log.e(TAG, "createMoment error");
                return;
            }
            Log.e(TAG, "createMoment successful");

            ret = startUpload(clip.getVdbId(), uploadUrl, dataType);
            if (ret != 0 && ret != CrsCommand.RES_FILE_TRANS_COMPLETE) {
                Log.e(TAG, "StartUpload error: " + ret);
                return;
            }
            if (ret == CrsCommand.RES_FILE_TRANS_COMPLETE) {
                Log.e(TAG, "Already exists. Upload Data successful");
            } else {
                Log.e(TAG, "Start uploading....");
                ret = uploadMomentData(clip, uploadUrl, dataType);
            }
            if (ret != CrsCommand.RES_FILE_TRANS_COMPLETE) {
                Log.e(TAG, "Upload fragment error: " + ret);
                return;
            }
            Log.e(TAG, "Upload fragment successful");

            ret = uploadThumbnail(clip);

            if (ret != CrsCommand.RES_FILE_TRANS_COMPLETE) {
                Log.e(TAG, "Upload thumbnail error: " + ret);
                return;
            }
            Log.e(TAG, "Upload thumbnail successful");
        } catch (IOException e) {
            Log.e(TAG, "", e);
        } finally {
            try {
                logOut();
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }
    }
}
