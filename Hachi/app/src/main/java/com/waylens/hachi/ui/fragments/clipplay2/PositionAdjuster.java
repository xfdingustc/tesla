package com.waylens.hachi.ui.fragments.clipplay2;

import com.waylens.hachi.vdb.urls.VdbUrl;

/**
 * Created by Xiaofei on 2016/2/24.
 */
public class PositionAdjuster {
    private static final String TAG = PositionAdjuster.class.getSimpleName();
    private final VdbUrl mUrl;
    private long mInitPosition;

    public PositionAdjuster(VdbUrl url) {
        this.mUrl = url;
    }

    public int getAdjustedPostion(int position) {
        int adjustedPosition = position;

        if (mInitPosition == 0 && position != 0) {
            mInitPosition = position;
        }

        adjustedPosition -= mInitPosition;

//        if (mUrl.realTimeMs != 0 && mInitPosition == 0 && position != 0
//            && Math.abs(mUrl.realTimeMs - position) < 200) {
//            mInitPosition = mUrl.realTimeMs;
//            Logger.t(TAG).d("setProgress - deviation: " + Math.abs(mUrl.realTimeMs - position));
//        }
//
//
//        if (mInitPosition == 0) {
//            adjustedPosition = position + (int) mUrl.realTimeMs;
//        }

        return adjustedPosition;
    }
}
