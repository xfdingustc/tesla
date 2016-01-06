package com.waylens.hachi.vdb;

/**
 * Created by Xiaofei on 2016/1/6.
 */
public class RawDataItem {
    public static final int RAW_DATA_NULL = 0;
    public static final int RAW_DATA_GPS = 1;
    public static final int RAW_DATA_ACC = 2;
    public static final int RAW_DATA_ODB = 3;

    private final int mType;
    private final long mPtsMs;
    public Object object; // GPSRawData for RAW_DATA_GPS

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
