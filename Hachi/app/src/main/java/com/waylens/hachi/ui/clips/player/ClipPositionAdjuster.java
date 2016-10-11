package com.waylens.hachi.ui.clips.player;


import com.waylens.hachi.snipe.vdb.urls.VdbUrl;

/**
 * Created by Xiaofei on 2016/2/24.
 */
public class ClipPositionAdjuster extends PositionAdjuster {
    private static final String TAG = ClipPositionAdjuster.class.getSimpleName();
    private final VdbUrl mUrl;
    private final long mStartTimeMs;


    public ClipPositionAdjuster(long  startTime, VdbUrl url) {
        this.mStartTimeMs = startTime;
        this.mUrl = url;
    }

    @Override
    public int getAdjustedPostion(int position) {
        int adjustedPosition = super.getAdjustedPostion(position);

        adjustedPosition += mUrl.realTimeMs - mStartTimeMs;

        return adjustedPosition;

    }
}
