package com.waylens.hachi.ui.clips.player;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.reative.SnipeApiRx;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlExRequest;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.Vdb;
import com.waylens.hachi.snipe.vdb.urls.PlaybackUrl;
import com.waylens.hachi.snipe.vdb.urls.VdbUrl;

import java.util.concurrent.ExecutionException;

import rx.Observable;
import rx.functions.Action1;

/**
 * Created by Xiaofei on 2016/2/22.
 */
public class ClipUrlProvider implements UrlProvider {
    private static final String TAG = ClipUrlProvider.class.getSimpleName();

    private Clip.ID mCid;
    private long mStartTime;

    private int maxLength;

    private PositionAdjuster mPositionAdjuster;

    public ClipUrlProvider(Clip.ID cid, long startTime, int maxLength) {
        this.mCid = cid;
        this.mStartTime = startTime;
        this.maxLength = maxLength;
    }


    @Override
    public Observable<PlaybackUrl> getUrlRx(long clipTimeMs) {
        return SnipeApiRx.getClipPlaybackUrl(mCid, mStartTime, clipTimeMs, maxLength)
            .doOnNext(new Action1<PlaybackUrl>() {
                @Override
                public void call(PlaybackUrl playbackUrl) {
                    mPositionAdjuster = new ClipPositionAdjuster(mStartTime, playbackUrl);
                }
            });
    }

    @Override
    public PositionAdjuster getPostionAdjuster() {
        return mPositionAdjuster;
    }
}
