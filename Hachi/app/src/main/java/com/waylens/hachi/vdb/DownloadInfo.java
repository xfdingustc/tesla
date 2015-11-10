package com.waylens.hachi.vdb;

/**
 * Created by Xiaofei on 2015/8/28.
 */
public class DownloadInfo {

    public static class DownloadStreamInfo {
        public int clipDate;
        public long clipTimeMs;
        public int lengthMs;
        public long size;
        public String url;
    }

    public final Clip.ID cid;
    public int opt;
    public final DownloadStreamInfo main = new DownloadStreamInfo();
    public final DownloadStreamInfo sub = new DownloadStreamInfo();
    public byte[] posterData;

    public DownloadInfo(Clip.ID cid) {
        this.cid = cid;
    }
}
