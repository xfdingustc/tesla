package com.waylens.hachi.ui.clips.player;


import com.xfdingustc.snipe.vdb.urls.VdbUrl;

/**
 * Created by Xiaofei on 2016/2/29.
 */
public interface UrlProvider {
    VdbUrl getUriSync(long clipTimeMs);
}
