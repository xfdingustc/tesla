package com.waylens.hachi.vdb;

/**
 * Created by Xiaofei on 2016/3/30.
 */
public class ClipSetPos {
    private final int mClipIndex;
    private final long mClipTimeMs;
    public ClipSetPos(int index, long timeMs) {
        this.mClipIndex = index;
        this.mClipTimeMs = timeMs;
    }

    public int getClipIndex() {
        return mClipIndex;
    }

    public long getClipTimeMs() {
        return mClipTimeMs;
    }

}
