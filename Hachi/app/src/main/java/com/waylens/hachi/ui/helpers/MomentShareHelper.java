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
import com.waylens.hachi.utils.DataUploaderV2;
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
import java.util.concurrent.CountDownLatch;

import crs_svr.ProtocolConstMsg;
import crs_svr.v2.CrsCommand;

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

    String tmpDataURL;

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
        createMoment();
    }
    void createMoment() {
        JSONObject params = new JSONObject();
        try {
            params.put("title", "Richard's Moment for testing");
            JSONArray hashTags = new JSONArray();
            hashTags.put("Shanghai");
            params.put("hashTags", hashTags);
            params.put("accessLevel", "PUBLIC");
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
                            uploadDataTask();
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

    void uploadDataTask() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean done = uploadData();
                if (done) {
                    mShareListener.onShareSuccessful();
                }
            }
        }).start();

    }

    boolean uploadData() {
        try {
            //mMomentInfo = new JSONObject("{\"momentID\":1033,\"uploadServer\":{\"privateKey\":\"qwertyuiopasdfgh\",\"port\":35020,\"ip\":\"192.168.20.160\"}}");
            JSONObject uploadServer = mMomentInfo.optJSONObject("uploadServer");
            String ip = uploadServer.optString("ip");
            int port = uploadServer.optInt("port");
            String privateKey = uploadServer.optString("privateKey");
            long momentID = mMomentInfo.optLong("momentID");
            DataUploaderV2 uploaderV2 = new DataUploaderV2(ip, port, privateKey);
            int dataType = CrsCommand.VIDIT_VIDEO_DATA_LOW | CrsCommand.VIDIT_RAW_DATA;
            uploaderV2.test(mVdbRequestQueue, momentID, mClip, dataType);
            return true;
        } catch (Exception e) {
            mShareListener.onError(103, R.string.share_upload_error);
            Log.e("test", "", e);
            return false;
        }
    }


    public interface OnShareMomentListener {
        void onShareSuccessful();

        void onCancelShare();

        void onError(int errorCode, int errorResId);
    }
}
