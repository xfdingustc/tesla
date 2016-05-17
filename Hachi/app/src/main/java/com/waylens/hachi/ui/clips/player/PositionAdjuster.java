package com.waylens.hachi.ui.clips.player;

/**
 * Created by Xiaofei on 2016/3/1.
 */
public abstract class PositionAdjuster {
    private long mInitPosition;

    public int getAdjustedPostion(int position) {
        int adjustedPosition = position;

        if (mInitPosition == 0 && position != 0) {
            mInitPosition = position;
        }

        adjustedPosition -= mInitPosition;

        return adjustedPosition;
    }
}
