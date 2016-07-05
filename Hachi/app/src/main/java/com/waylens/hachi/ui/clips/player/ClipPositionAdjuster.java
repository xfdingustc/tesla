package com.waylens.hachi.ui.clips.player;

import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.urls.VdbUrl;

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

//        Logger.t(TAG).d("startTime: " + mSharableClip.getStartTimeMs() + " realTime: " + mUrl.realTimeMs);
        adjustedPosition += mUrl.realTimeMs - mStartTimeMs;

        return adjustedPosition;

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


    }
}
