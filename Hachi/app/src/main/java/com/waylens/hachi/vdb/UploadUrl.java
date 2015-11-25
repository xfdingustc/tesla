package com.waylens.hachi.vdb;

/**
 * Created by Richard on 11/19/15.
 */
public class UploadUrl {
    public boolean isPlayList;

    public Clip.ID cid;

    public long realTimeMs;

    public int lengthMs;

    public int uploadOpt;

    public String url;

    public UploadUrl(boolean isPlayList, Clip.ID cid, long realTimeMs, int lengthMs, int uploadOpt, String url) {
        this.isPlayList = isPlayList;
        this.cid = cid;
        this.realTimeMs = realTimeMs;
        this.lengthMs = lengthMs;
        this.uploadOpt = uploadOpt;
        this.url = url;
    }
}
