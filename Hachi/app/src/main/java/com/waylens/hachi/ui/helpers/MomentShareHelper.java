package com.waylens.hachi.ui.helpers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.utils.DataUploaderV2;
import com.waylens.hachi.utils.VolleyUtil;
import com.waylens.hachi.vdb.ClipSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;

import crs_svr.v2.CrsCommand;

/**
 * Created by Richard on 1/5/16.
 */
public class MomentShareHelper {
    VdbRequestQueue mVdbRequestQueue;
    RequestQueue mRequestQueue;
    OnShareMomentListener mShareListener;

    Thread mUploadThread;
    volatile DataUploaderV2 uploaderV2;

    volatile boolean isCancelled;

    Handler mHandler;

    public MomentShareHelper(Context context, OnShareMomentListener listener) {
        mVdbRequestQueue = Snipe.newRequestQueue();
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(context);
        mShareListener = listener;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public void cancel(boolean cleanListener) {
        isCancelled = true;
        if (mUploadThread != null && mUploadThread.isAlive() && uploaderV2 != null) {
            uploaderV2.cancel();
        }
        if (cleanListener) {
            mShareListener = null;
        }
    }

    public void shareMoment(SharableClip sharableClip, String title, String[] tags, String accessLevel) {
        uploadDataTask(new SharableClip[]{sharableClip}, title, tags, accessLevel);
    }

    public void shareMoment(int playListID, String title, String[] tags, String accessLevel) {
        uploadDataTask(playListID, title, tags, accessLevel);
    }

    JSONObject createMoment(String title, String[] tags, String accessLevel) {
        final CountDownLatch latch = new CountDownLatch(1);
        final JSONObject[] results = new JSONObject[]{null};

        JSONObject params = new JSONObject();
        try {
            params.put("title", title);
            JSONArray hashTags = new JSONArray();
            for (String tag : tags) {
                hashTags.put(tag);
            }
            params.put("hashTags", hashTags);
            params.put("accessLevel", accessLevel);
            Log.e("test", "params: " + params);
        } catch (JSONException e) {
            Log.e("test", "", e);
        }

        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.POST, Constants.API_MOMENTS, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        results[0] = response;
                        latch.countDown();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("test", "", error);
                        latch.countDown();
                    }
                }));

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e("test", "", e);
        }
        return results[0];
    }

    void uploadDataTask(final SharableClip[] sharableClips, final String title, final String[] tags, final String accessLevel) {
        mUploadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int result = uploadData(sharableClips, title, tags, accessLevel);
                uploaderV2 = null;
                mUploadThread = null;
                if (mShareListener != null) {
                    processResult(result);
                }
            }
        });
        mUploadThread.start();
    }

    void uploadDataTask(final int playListID, final String title, final String[] tags, final String accessLevel) {
        mUploadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ClipSet playList = retrievePlayListInfo(playListID);
                SharableClip[] sharableClips = new SharableClip[playList.getCount()];

                for (int i = 0; i < sharableClips.length; i++) {
                    sharableClips[i] = new SharableClip(playList.getClip(i));
                }

                int result = uploadData(sharableClips, title, tags, accessLevel);
                uploaderV2 = null;
                mUploadThread = null;
                if (mShareListener != null) {
                    processResult(result);
                }
            }
        });
        mUploadThread.start();
    }

    ClipSet retrievePlayListInfo(int type) {
        final CountDownLatch latch = new CountDownLatch(1);
        final ClipSet[] clipSets = new ClipSet[]{null};

        mVdbRequestQueue.add(new ClipSetRequest(type, ClipSetRequest.FLAG_CLIP_EXTRA,
                new VdbResponse.Listener<ClipSet>() {
                    @Override
                    public void onResponse(ClipSet clipSet) {
                        clipSets[0] = clipSet;
                        latch.countDown();
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Log.e("test", "", error);
                        latch.countDown();
                    }
                }));
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e("test", "", e);
        }
        return clipSets[0];
    }

    void processResult(final int result) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (result == CrsCommand.RES_STATE_OK) {
                    mShareListener.onShareSuccessful();
                } else if (result == CrsCommand.RES_STATE_FAIL) {
                    mShareListener.onError(result, 0);
                } else {
                    mShareListener.onCancelShare();
                }
            }
        });
    }

    int uploadData(SharableClip[] sharableClips, String title, String[] tags, String accessLevel) {
        try {
            if (isCancelled) {
                return CrsCommand.RES_STATE_CANCELLED;
            }
            JSONObject momentInfo = createMoment(title, tags, accessLevel);
            if (isCancelled) {
                return CrsCommand.RES_STATE_CANCELLED;
            }
            JSONObject uploadServer = momentInfo.optJSONObject("uploadServer");
            String ip = uploadServer.optString("ip");
            int port = uploadServer.optInt("port");
            String privateKey = uploadServer.optString("privateKey");
            long momentID = momentInfo.optLong("momentID");
            uploaderV2 = new DataUploaderV2(ip, port, privateKey, mVdbRequestQueue);
            int dataType = CrsCommand.VIDIT_VIDEO_DATA_LOW | CrsCommand.VIDIT_RAW_DATA;
            return uploaderV2.upload(momentID, sharableClips, dataType);
        } catch (Exception e) {
            Log.e("test", "", e);
            return CrsCommand.RES_STATE_FAIL;
        }
    }


    public interface OnShareMomentListener {
        void onShareSuccessful();

        void onCancelShare();

        void onError(int errorCode, int errorResId);

        void onUploadProgress(int uploadPercentage);
    }
}
