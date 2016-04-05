package com.waylens.hachi.ui.fragments.clipplay2;

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
