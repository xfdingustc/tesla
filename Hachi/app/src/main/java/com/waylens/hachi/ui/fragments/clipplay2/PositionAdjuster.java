package com.waylens.hachi.ui.fragments.clipplay2;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.urls.VdbUrl;

/**
 * Created by Xiaofei on 2016/2/24.
 */
public class PositionAdjuster {
    private static final String TAG = PositionAdjuster.class.getSimpleName();
    private final VdbUrl mUrl;
    private final Clip mClip;
    private long mInitPosition;

    public PositionAdjuster(Clip clip, VdbUrl url) {
        this.mClip = clip;
        this.mUrl = url;
    }

    public int getAdjustedPostion(int position) {
        int adjustedPosition = position;

        if (mInitPosition == 0 && position != 0) {
            mInitPosition = position;
        }

        adjustedPosition -= mInitPosition;

//        Logger.t(TAG).d("startTime: " + mSharableClip.getStartTimeMs() + " realTime: " + mUrl.realTimeMs);
        adjustedPosition += mUrl.realTimeMs - mClip.getStartTimeMs();

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
