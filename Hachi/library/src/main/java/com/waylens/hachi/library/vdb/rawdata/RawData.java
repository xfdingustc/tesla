package com.waylens.hachi.library.vdb.rawdata;

import java.io.Serializable;

/**
 * Created by Xiaofei on 2016/4/12.
 */
public class RawData implements Serializable {
    private long mPts;
    private IioData mIioData;
    private GpsData mGpsData;
    private ObdData mObdData;

    public RawData() {

    }

    public void setPts(long pts) {
        mPts = pts;
    }

    public void setIioData(IioData iioData) {
        mIioData = iioData;
    }

    public void setGpsData(GpsData gpsData) {
        mGpsData = gpsData;
    }

    public void setObdData(ObdData obdData) {
        mObdData = obdData;
    }

    public long getPts() {
        return mPts;
    }

    public IioData getIioData() {
        return mIioData;
    }

    public GpsData getGpsData() {
        return mGpsData;
    }

    public ObdData getObdData() {
        return mObdData;
    }
}
