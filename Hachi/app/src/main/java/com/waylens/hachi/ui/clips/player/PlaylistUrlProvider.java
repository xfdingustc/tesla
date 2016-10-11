package com.waylens.hachi.ui.clips.player;


import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.reative.SnipeApiRx;
import com.waylens.hachi.snipe.toolbox.PlaylistPlaybackUrlRequest;
import com.waylens.hachi.snipe.vdb.urls.PlaylistPlaybackUrl;
import com.waylens.hachi.snipe.vdb.urls.VdbUrl;

import rx.Observable;
import rx.functions.Action1;

/**
 * Created by Xiaofei on 2016/2/29.
 */
public class PlaylistUrlProvider implements UrlProvider {
    private static final String TAG = PlaylistUrlProvider.class.getSimpleName();

    private final int mPlayListID;

    private PositionAdjuster mPositionAdjuster;


    public PlaylistUrlProvider(int playListID) {
        mPlayListID = playListID;
    }


    @Override
    public Observable<PlaylistPlaybackUrl> getUrlRx(long clipTimeMs) {
        return SnipeApiRx.getPlaylistPlaybackUrl(mPlayListID, (int)clipTimeMs)
            .doOnNext(new Action1<PlaylistPlaybackUrl>() {
                @Override
                public void call(PlaylistPlaybackUrl playlistPlaybackUrl) {
                    mPositionAdjuster = new PlaylistPositionAdjuster(playlistPlaybackUrl);
                }
            });
    }

    @Override
    public PositionAdjuster getPostionAdjuster() {
        return mPositionAdjuster;
    }


}
