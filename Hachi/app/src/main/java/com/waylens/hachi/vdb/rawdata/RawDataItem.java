package com.waylens.hachi.vdb.rawdata;

import android.util.Log;

import com.waylens.hachi.utils.Utils;


/**
 * Created by Xiaofei on 2016/1/6.
 */
public class RawDataItem {
    public static final int DATA_TYPE_UNKNOWN = 0;
    public static final int DATA_TYPE_GPS = 1;
    public static final int DATA_TYPE_IIO = 2;
    public static final int DATA_TYPE_OBD = 3;

    private final int mType;
    private final long mPtsMs;
    public Object data;

    public RawDataItem(int type, long ptsMs) {
        this.mType = type;
        this.mPtsMs = ptsMs;
    }

    public int getType() {
        return mType;
    }

    public long getPtsMs() {
        return mPtsMs;
    }

}
