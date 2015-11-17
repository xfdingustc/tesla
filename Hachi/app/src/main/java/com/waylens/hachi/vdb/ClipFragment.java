package com.waylens.hachi.vdb;

/**
 * Created by Xiaofei on 2015/8/27.
 */
public class ClipFragment {
    private final Clip mClip;
    private final long mStartTimeMs;
    private final long mEndTimeMs;

    public ClipFragment(Clip clip, long startTimeMs, long endTimeMs) {
        this.mClip = clip;
        this.mStartTimeMs = startTimeMs;
        this.mEndTimeMs = endTimeMs;
    }

    public Clip getClip() {
        return mClip;
    }

    public long getStartTimeMs() {
        return mStartTimeMs;
    }

    public int getDurationMs() {
        return (int)(mEndTimeMs - mStartTimeMs);
    }
}
