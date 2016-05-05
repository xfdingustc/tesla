package com.waylens.hachi.ui.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.toolbox.ClipUploadUrlRequest;
import com.waylens.hachi.snipe.toolbox.VdbImageRequest;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.utils.VolleyUtil;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.urls.UploadUrl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import crs_svr.v2.CrsCommand;

/**
 * Created by Richard on 3/10/16.
 */
public class MomentBuilder {

    private static final String TAG = MomentBuilder.class.getSimpleName();

    private static final int DEFAULT_DATA_TYPE_CAM = VdbCommand.Factory.UPLOAD_GET_V1 | VdbCommand.Factory.UPLOAD_GET_RAW;

    private static final int DEFAULT_DATA_TYPE_CLOUD = CrsCommand.VIDIT_VIDEO_DATA_LOW | CrsCommand.VIDIT_RAW_DATA;


    private LocalMoment mLocalMoment;

    OnBuildListener mOnBuildListener;

    VdbRequestQueue mVdbRequestQueue;

    RequestQueue mVolleyRequestQueue;

    int mPlayListID;

    ClipSet mClipSet;
    ArrayList<LocalMoment.Segment> mSegmentList = new ArrayList<>();
    String mThumbnailPath;

    int requestCounter;

    File cacheDir;

    volatile boolean isCancelled;

    public MomentBuilder(Context context, VdbRequestQueue vdbRequestQueue) {
        mVdbRequestQueue = vdbRequestQueue;
        mVolleyRequestQueue = VolleyUtil.newVolleyRequestQueue(context);
        cacheDir = context.getExternalCacheDir();
    }

    public MomentBuilder forPlayList(int playListID) {
        mPlayListID = playListID;
        return this;
    }

    public MomentBuilder asMoment(String title, String[] tags, String accessLevel, int audioID, JSONObject gaugeSettings) {
        mLocalMoment = new LocalMoment(title, tags, accessLevel, audioID, gaugeSettings);
        return this;
    }

    public void build(OnBuildListener listener) {
        mOnBuildListener = listener;
        retrievePlayListInfo();
    }

    void retrievePlayListInfo() {
        if (isCancelled) {
            mOnBuildListener.onCancelBuild();
            return;
        }

        Logger.t(TAG).d("retrievePlayListInfo: " + mPlayListID);
        mVdbRequestQueue.add(new ClipSetExRequest(mPlayListID, ClipSetExRequest.FLAG_CLIP_EXTRA,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet clipSet) {
                    if (clipSet == null || clipSet.getCount() == 0) {
                        mOnBuildListener.onBuildError(MomentShareHelper.ERROR_GET_CLIP_SET, 0);
                    } else {

                        mClipSet = clipSet;
                        mSegmentList.clear();
                        requestCounter = 0;
                        retrieveUploadURL();
                    }
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    mOnBuildListener.onBuildError(MomentShareHelper.ERROR_GET_CLIP_SET, 0);
                }
            }));
    }

    void retrieveUploadURL() {
        if (isCancelled) {
            mOnBuildListener.onCancelBuild();
            return;
        }

        Logger.t(TAG).d("retrieveUploadURL");
        if (requestCounter >= mClipSet.getCount()) {
            getClipThumbnail();
            return;
        }
        final Clip clip = mClipSet.getClip(requestCounter);

        Bundle parameters = new Bundle();
        parameters.putBoolean(ClipUploadUrlRequest.PARAM_IS_PLAY_LIST, false);
        parameters.putLong(ClipUploadUrlRequest.PARAM_CLIP_TIME_MS, clip.getStartTimeMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_CLIP_LENGTH_MS, clip.getDurationMs());
        parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, DEFAULT_DATA_TYPE_CAM);

        ClipUploadUrlRequest request = new ClipUploadUrlRequest(clip.cid, parameters,
            new VdbResponse.Listener<UploadUrl>() {
                @Override
                public void onResponse(UploadUrl uploadUrl) {
                    requestCounter++;
                    LocalMoment.Segment segment = new LocalMoment.Segment(clip, uploadUrl, DEFAULT_DATA_TYPE_CLOUD);
                    mSegmentList.add(segment);
                    retrieveUploadURL();
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    mOnBuildListener.onBuildError(MomentShareHelper.ERROR_GET_UPLOAD_URL, 0);
                }
            });
        mVdbRequestQueue.add(request);
    }

    void getClipThumbnail() {
        if (isCancelled) {
            mOnBuildListener.onCancelBuild();
            return;
        }

        Logger.t(TAG).d("getClipThumbnail");
        final Clip clip = mClipSet.getClip(0);
        ClipPos clipPos = new ClipPos(clip.getVdbId(), clip.cid, clip.getDate(), clip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
        VdbImageRequest request = new VdbImageRequest(
            clipPos,
            new VdbResponse.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap Bitmap) {
                    File file = new File(cacheDir, "t" + clip.getStartTimeMs() + ".jpg");
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(file);
                        Bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, fos);
                        mThumbnailPath = file.getAbsolutePath();
                        createMoment();
                    } catch (FileNotFoundException e) {
                        mOnBuildListener.onBuildError(MomentShareHelper.ERROR_CACHE_THUMBNAIL, 0);
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                //
                            }
                        }
                    }
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    mOnBuildListener.onBuildError(MomentShareHelper.ERROR_GET_THUMBNAIL, 0);
                }
            },
            0, 0, ImageView.ScaleType.CENTER_INSIDE, Bitmap.Config.RGB_565, null
        );
        mVdbRequestQueue.add(request);
    }

    void prepareUpload() {
        if (isCancelled) {
            mOnBuildListener.onCancelBuild();
            return;
        }

        Logger.t(TAG).d("prepareUpload");
        mLocalMoment.setFragments(mSegmentList, mThumbnailPath);
        mLocalMoment.setPrepared(true);
        mOnBuildListener.onBuildSuccessful(mLocalMoment);
    }

    void createMoment() {
        if (isCancelled) {
            mOnBuildListener.onCancelBuild();
            return;
        }

        Logger.t(TAG).d("createMoment");
        JSONObject params = new JSONObject();
        try {
            params.put("title", mLocalMoment.title);
            JSONArray hashTags = new JSONArray();
            for (String tag : mLocalMoment.tags) {
                hashTags.put(tag);
            }
            params.put("hashTags", hashTags);
            params.put("accessLevel", mLocalMoment.accessLevel);
            if (mLocalMoment.audioID > 0) {
                params.put("audioType", 1);
                params.put("musicSource", "" + mLocalMoment.audioID);
            } else {
                params.put("audioType", 0);
            }

            params.put("overlay", mLocalMoment.gaugeSettings);
            Logger.t(TAG).d("params: " + params);
        } catch (JSONException e) {
            Logger.t(TAG).e("", e);
        }

        mVolleyRequestQueue.add(new AuthorizedJsonRequest(Request.Method.POST, Constants.API_MOMENTS, params,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject momentInfo) {
                    JSONObject uploadServer = momentInfo.optJSONObject("uploadServer");
                    String ip = uploadServer.optString("ip");
                    int port = uploadServer.optInt("port");
                    String privateKey = uploadServer.optString("privateKey");
                    long momentID = momentInfo.optLong("momentID");
                    mLocalMoment.updateUploadInfo(momentID, ip, port, privateKey);
                    prepareUpload();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mOnBuildListener.onBuildError(MomentShareHelper.ERROR_CREATE_MOMENT, 0);
                }
            }));
    }

    public void cancel() {
        isCancelled = true;
    }

    public interface OnBuildListener {
        void onBuildSuccessful(LocalMoment localMoment);

        void onBuildError(int errorCode, int messageResID);

        void onCancelBuild();
    }
}
