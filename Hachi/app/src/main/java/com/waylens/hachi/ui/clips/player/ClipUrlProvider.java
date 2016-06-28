package com.waylens.hachi.ui.clips.player;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlExRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.Vdb;
import com.waylens.hachi.vdb.urls.PlaybackUrl;
import com.waylens.hachi.vdb.urls.VdbUrl;

import java.util.concurrent.ExecutionException;

/**
 * Created by Xiaofei on 2016/2/22.
 */
public class ClipUrlProvider implements UrlProvider {
    private static final String TAG = ClipUrlProvider.class.getSimpleName();
    private VdbRequestQueue mVdbRequestQueue;
    private OnUrlLoadedListener mListener;
    private Clip.ID mCid;
    private long mStartTime;

    private int maxLength;

    public ClipUrlProvider(@NonNull VdbRequestQueue requestQueue, Clip.ID cid, long startTime, int maxLength) {
        this.mVdbRequestQueue = requestQueue;
        this.mCid = cid;
        this.mStartTime = startTime;
        this.maxLength = maxLength;
    }

    @Override
    public void getUri(long clipTimeMs, OnUrlLoadedListener listener) {
        Logger.t(TAG).d("Start load clip url clipTimeMs: " + (clipTimeMs + mStartTime));

        mListener = listener;


        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlExRequest.PARAMETER_URL_TYPE, Vdb.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlExRequest.PARAMETER_STREAM, Vdb.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlExRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlExRequest.PARAMETER_CLIP_TIME_MS, clipTimeMs + mStartTime);
        parameters.putInt(ClipPlaybackUrlExRequest.PARAMETER_CLIP_LENGTH_MS, maxLength);

        ClipPlaybackUrlExRequest request = new ClipPlaybackUrlExRequest(mCid,
                parameters, new
                VdbResponse.Listener<PlaybackUrl>() {
                    @Override
                    public void onResponse(PlaybackUrl playbackUrl) {
                        if (mListener != null) {
                            mListener.onUrlLoaded(playbackUrl);
                        }

                    }
                }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Logger.t(TAG).d("", error);
                if (mListener != null) {
                    mListener.onUrlLoaded(null);
                }
            }
        });

        mVdbRequestQueue.add(request);
    }

    @Override
    public VdbUrl getUriSync(long clipTimeMs) {
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlExRequest.PARAMETER_URL_TYPE, Vdb.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlExRequest.PARAMETER_STREAM, Vdb.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlExRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlExRequest.PARAMETER_CLIP_TIME_MS, clipTimeMs + mStartTime);
        parameters.putInt(ClipPlaybackUrlExRequest.PARAMETER_CLIP_LENGTH_MS, maxLength);


        VdbRequestFuture<PlaybackUrl> requestFuture = VdbRequestFuture.newFuture();
        ClipPlaybackUrlExRequest request = new ClipPlaybackUrlExRequest(mCid, parameters, requestFuture, requestFuture);
        mVdbRequestQueue.add(request);
        try {
            PlaybackUrl playbackUrl = requestFuture.get();
            return playbackUrl;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }


    }
}
