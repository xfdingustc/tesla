package com.waylens.hachi.ui.clips.player;


import com.waylens.hachi.snipe.vdb.urls.VdbUrl;

import rx.Observable;

/**
 * Created by Xiaofei on 2016/2/29.
 */
public interface UrlProvider {
    Observable<? extends VdbUrl> getUrlRx(long clipTimeMs);

    PositionAdjuster getPostionAdjuster();
}
