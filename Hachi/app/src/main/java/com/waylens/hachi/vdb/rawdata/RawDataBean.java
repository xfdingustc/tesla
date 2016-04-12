package com.waylens.hachi.vdb.rawdata;

import java.io.Serializable;

/**
 * Created by Xiaofei on 2016/4/12.
 */
public class RawDataBean implements Serializable{
    private long mPts;
    private int mAccX;
    private int mAccY;
    private int mAccZ;

    private int mGyroX;
    private int mGyroY;
    private int mGyroZ;

    private int mMagnX;
    private int mMagnY;
    private int mMagnZ;

    public RawDataBean(RawDataItem iio, RawDataItem gps, RawDataItem obd) {
        if (iio != null) {
            mPts = iio.getPtsMs();
            IioData iioData = (IioData)iio.data;
            mAccX = iioData.accX;
            mAccY = iioData.accY;
            mAccZ = iioData.accZ;
            mGyroX = iioData.gyro_x;
            mGyroY = iioData.gyro_y;
            mGyroZ = iioData.gyro_z;

        }
    }
}
