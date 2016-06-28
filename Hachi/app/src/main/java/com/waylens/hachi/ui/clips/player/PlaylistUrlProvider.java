package com.waylens.hachi.ui.clips.player;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.PlaylistPlaybackUrlRequest;
import com.waylens.hachi.vdb.urls.PlaylistPlaybackUrl;
import com.waylens.hachi.vdb.urls.VdbUrl;

import java.util.concurrent.ExecutionException;

/**
 * Created by Xiaofei on 2016/2/29.
 */
public class PlaylistUrlProvider implements UrlProvider {
    private static final String TAG = PlaylistUrlProvider.class.getSimpleName();
    private final VdbRequestQueue mVdbRequestQueue;
    private final int mPlayListID;
    private OnUrlLoadedListener mListener;

    public PlaylistUrlProvider(VdbRequestQueue requestQueue, int playListID) {
        this.mVdbRequestQueue = requestQueue;
        mPlayListID = playListID;
    }

    @Override
    public void getUri(long clipTimeMs, OnUrlLoadedListener listener) {
        mListener = listener;
        doGetPlaylistUri(clipTimeMs);
    }

    @Override
    public VdbUrl getUriSync(long clipTimeMs) {
        VdbRequestFuture<PlaylistPlaybackUrl> requestFuture = VdbRequestFuture.newFuture();
        PlaylistPlaybackUrlRequest request = new PlaylistPlaybackUrlRequest(mPlayListID, (int)clipTimeMs, requestFuture, requestFuture);
        mVdbRequestQueue.add(request);

        try {
            PlaylistPlaybackUrl url = requestFuture.get();
            return url;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void doGetPlaylistUri(long clipTimeMs) {
        PlaylistPlaybackUrlRequest request = new PlaylistPlaybackUrlRequest(mPlayListID,
                (int)clipTimeMs, new VdbResponse.Listener<PlaylistPlaybackUrl>() {
            @Override
            public void onResponse(PlaylistPlaybackUrl response) {

                Logger.t(TAG).d("Get playlist: " + response.url);
                if (mListener != null) {
                    mListener.onUrlLoaded(response);
                }
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
            }
        });
        mVdbRequestQueue.add(request);
    }
}
