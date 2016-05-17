package com.waylens.hachi.ui.clips.player;

import com.waylens.hachi.vdb.urls.VdbUrl;

/**
 * Created by Xiaofei on 2016/2/29.
 */
public interface UrlProvider {
    interface OnUrlLoadedListener {
        void onUrlLoaded(VdbUrl url);
    }

    void getUri(long clipTimeMs, OnUrlLoadedListener listener);
}
