package com.waylens.hachi.vdb;

public class DownloadingClip extends LocalClip {

    public String outputFile;
    public byte[] posterData;
    public int progress; // downloading progress

    // subType: id
    // extra: null
    // numStreams: 1

    public DownloadingClip(int id, int duration) {
        super(TYPE_DOWNLOADING, id, null, 1);
        progress = -1;
        mDurationMs = duration;
    }

    @Override
    public boolean isDownloading() {
        return true;
    }

    @Override
    public int getDownloadProgress() {
        return progress;
    }

    public static final Clip.ID createClipId(int id) {
        return new Clip.ID(CAT_LOCAL, TYPE_DOWNLOADING, id, null);
    }

}
