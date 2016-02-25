package com.waylens.hachi.ui.fragments.clipplay2;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.Vdb;
import com.waylens.hachi.vdb.urls.PlaybackUrl;
import com.waylens.hachi.vdb.urls.VdbUrl;

/**
 * Created by Xiaofei on 2016/2/22.
 */
class VdtUriProvider {
    private static final String TAG = VdtUriProvider.class.getSimpleName();
    private VdbRequestQueue mVdbRequestQueue;
    private OnUriLoadedListener mListener;

    public interface OnUriLoadedListener {
        void onUriLoaded(VdbUrl url);
    }

    public VdtUriProvider(VdbRequestQueue requestQueue) {
        mVdbRequestQueue = requestQueue;
    }


    public void getUri(Clip clip, long clipTimeMs, OnUriLoadedListener listener) {
        Logger.t(TAG).d("Start load clip url:");
        mListener = listener;

        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, Vdb.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, Vdb.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, clipTimeMs);

        ClipPlaybackUrlRequest request = new ClipPlaybackUrlRequest(clip.cid, parameters, new
            VdbResponse.Listener<PlaybackUrl>() {
            @Override
            public void onResponse(PlaybackUrl playbackUrl) {
                if (mListener != null) {
                    mListener.onUriLoaded(playbackUrl);
                }

            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Logger.t(TAG).d("", error);
            }
        });

        mVdbRequestQueue.add(request);
    }
}
