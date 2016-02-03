package com.waylens.hachi.vdb.urls;

import com.waylens.hachi.vdb.Clip;

/**
 * Created by Xiaofei on 2015/8/28.
 */
public class PlaybackUrl extends VdbUrl{
    public final Clip.ID cid;
    public int stream;
    public int urlType;
    public long realTimeMs;

    public boolean bHasMore;

    public int offsetMs; // for local clip; for remote it is 0

    public PlaybackUrl(Clip.ID cid) {
        this.cid = cid;
    }
}
