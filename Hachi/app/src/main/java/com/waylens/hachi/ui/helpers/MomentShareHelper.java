package com.waylens.hachi.ui.helpers;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipUploadUrlRequest;
import com.waylens.hachi.utils.DataUploader;
import com.waylens.hachi.utils.VolleyUtil;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.UploadUrl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import crs_svr.ProtocolConstMsg;

/**
 * Created by Richard on 1/5/16.
 */
public class MomentShareHelper {

    UploadUrl mUploadUrlVideo;
    UploadUrl mUploadUrlRaw;
    JSONObject mMomentInfo;
    VdbRequestQueue mVdbRequestQueue;
    RequestQueue mRequestQueue;
    Clip mClip;

    OnShareMomentListener mShareListener;

    volatile boolean isCancelled;

    public MomentShareHelper(Context context, Clip clip, OnShareMomentListener listener) {
        mVdbRequestQueue = Snipe.newRequestQueue();
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(context);
        mClip = clip;
        mShareListener = listener;
    }

    public void cancel() {
        isCancelled = true;
    }

    public void shareMoment() {
        getUploadUrlVideo();
    }

    void getUploadUrlVideo() {
        Bundle parameters = new Bundle();
        parameters.putBoolean(ClipUploadUrlRequest.PARAM_IS_PLAY_LIST, false);
        parameters.putLong(ClipUploadUrlRequest.PARAM_CLIP_TIME_MS, mClip.getStartTimeMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_CLIP_LENGTH_MS, mClip.getDurationMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, VdbCommand.Factory.UPLOAD_GET_V1);

        ClipUploadUrlRequest request = new ClipUploadUrlRequest(mClip, parameters,
                new VdbResponse.Listener<UploadUrl>() {
                    @Override
                    public void onResponse(UploadUrl response) {
                        mUploadUrlVideo = response;
                        if (!isCancelled) {
                            getUploadUrlRaw();
                        } else {
                            mShareListener.onCancelShare();
                        }
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        mShareListener.onError(100, R.string.share_video_retrieve_error);
                        Log.e("test", "", error);
                    }
                });

        mVdbRequestQueue.add(request);
    }

    void createMoment() {
        JSONObject params = new JSONObject();
        try {
            params.put("title", "Moment from Android");
            JSONObject raw = new JSONObject();
            raw.put("guid", mClip.getVdbId());
            JSONArray rawArray = new JSONArray();
            rawArray.put(raw);
            params.put("rawData", rawArray);
            JSONObject fragment = new JSONObject();
            fragment.put("guid", mClip.getVdbId());
            fragment.put("clipCaptureTime", mClip.getDateTimeString());
            fragment.put("beginTime", mClip.getStartTimeMs());
            fragment.put("offset", mUploadUrlVideo.realTimeMs - mClip.getStartTimeMs());
            fragment.put("duration", mUploadUrlVideo.lengthMs);
            JSONArray fragments = new JSONArray();
            fragments.put(fragment);
            params.put("fragments", fragments);
            Log.e("test", "params: " + params);
        } catch (JSONException e) {
            Log.e("test", "", e);
        }

        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.POST, Constants.API_MOMENTS, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e("test", "response: " + response);
                        mMomentInfo = response;
                        if (!isCancelled) {
                            uploadData();
                        } else {
                            mShareListener.onCancelShare();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mShareListener.onError(101, R.string.create_moment_error);
                        Log.e("test", "", error);
                    }
                }));
    }

    void uploadData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean done = uploadData(mUploadUrlVideo.url, "video", ProtocolConstMsg.VIDIT_VIDEO_DATA_LOW);
                if (!done) {
                    return;
                }
                done = uploadData(mUploadUrlRaw.url, "raw", ProtocolConstMsg.VIDIT_RAW_DATA);
                if (done) {
                    mShareListener.onShareSuccessful();
                }
            }
        }).start();

    }

    boolean uploadData(String urlString, String type, int option) {
        InputStream inputStream = null;
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            inputStream = conn.getInputStream();
            Log.e("test", String.format("type[%s], ContentLength[%d]", type, conn.getContentLength()));
            JSONObject uploadServer = mMomentInfo.optJSONObject("uploadServer");
            String ip = uploadServer.optString("ip");
            int port = uploadServer.optInt("port");
            String privateKey = uploadServer.optString("privateKey");
            String[] tokenAndGuid = findTokenAndGuid(mMomentInfo, type);
            DataUploader uploader = new DataUploader(ip, port, privateKey);
            return uploader.uploadStream(inputStream, conn.getContentLength(), option, tokenAndGuid[0], tokenAndGuid[1]);
        } catch (Exception e) {
            mShareListener.onError(103, R.string.share_upload_error);
            return false;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e("test", "", e);
                }
            }
        }
    }

    String[] findTokenAndGuid(JSONObject momentInfo, String type) {
        JSONArray jsonArray = momentInfo.optJSONArray("uploadData");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.optJSONObject(i);
            if (type.equals(jsonObject.optString("dataType"))) {
                return new String[]{jsonObject.optString("uploadToken"), jsonObject.optString("guid")};
            }
        }
        return null;
    }

    void getUploadUrlRaw() {
        Bundle parameters = new Bundle();
        parameters.putBoolean(ClipUploadUrlRequest.PARAM_IS_PLAY_LIST, false);
        parameters.putLong(ClipUploadUrlRequest.PARAM_CLIP_TIME_MS, mClip.getStartTimeMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_CLIP_LENGTH_MS, mClip.getDurationMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, VdbCommand.Factory.UPLOAD_GET_RAW);

        ClipUploadUrlRequest request = new ClipUploadUrlRequest(mClip, parameters,
                new VdbResponse.Listener<UploadUrl>() {
                    @Override
                    public void onResponse(UploadUrl response) {
                        mUploadUrlRaw = response;
                        if (!isCancelled) {
                            createMoment();
                        } else {
                            mShareListener.onCancelShare();
                        }
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Log.e("test", "", error);
                        mShareListener.onError(102, R.string.share_raw_retrieve_error);
                    }
                });

        mVdbRequestQueue.add(request);
    }

    public interface OnShareMomentListener {
        void onShareSuccessful();

        void onCancelShare();

        void onError(int errorCode, int errorResId);
    }
}
