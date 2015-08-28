package com.waylens.hachi.vdb;

/**
 * Created by Xiaofei on 2015/8/28.
 */
public class PlaybackUrl {
    public final Clip.ID cid;
    public int stream;
    public int urlType;
    public long realTimeMs;
    public int lengthMs;
    public boolean bHasMore;
    public String url;
    public int offsetMs; // for local clip; for remote it is 0

    public PlaybackUrl(Clip.ID cid) {
        this.cid = cid;
    }
}
