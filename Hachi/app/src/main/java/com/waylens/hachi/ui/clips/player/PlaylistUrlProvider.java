package com.waylens.hachi.ui.clips.player;

import com.waylens.hachi.library.vdb.urls.PlaylistPlaybackUrl;
import com.waylens.hachi.library.vdb.urls.VdbUrl;
import com.waylens.hachi.library.snipe.VdbRequestFuture;
import com.waylens.hachi.library.snipe.VdbRequestQueue;
import com.waylens.hachi.library.snipe.toolbox.PlaylistPlaybackUrlRequest;


/**
 * Created by Xiaofei on 2016/2/29.
 */
public class PlaylistUrlProvider implements UrlProvider {
    private static final String TAG = PlaylistUrlProvider.class.getSimpleName();
    private final VdbRequestQueue mVdbRequestQueue;
    private final int mPlayListID;


    public PlaylistUrlProvider(VdbRequestQueue requestQueue, int playListID) {
        this.mVdbRequestQueue = requestQueue;
        mPlayListID = playListID;
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


}
