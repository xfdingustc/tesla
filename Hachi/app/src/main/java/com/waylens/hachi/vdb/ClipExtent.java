package com.waylens.hachi.vdb;

/**
 * Created by Richard on 9/15/15.
 */
public class ClipExtent {
    public final Clip.ID cid;

    public final Clip.ID originalCid;

    public long minClipStartTimeMs;

    public long maxClipEndTimeMs;

    public long clipStartTimeMs;

    public long clipEndTimeMs;

    public ClipExtent(Clip.ID cid, Clip.ID originalCid) {
        this.cid = cid;
        this.originalCid = originalCid;
    }
}
