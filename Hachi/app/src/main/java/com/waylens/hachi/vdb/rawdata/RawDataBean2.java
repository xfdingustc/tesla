package com.waylens.hachi.vdb.rawdata;

import java.io.Serializable;

/**
 * Created by Xiaofei on 2016/4/12.
 */
public class RawDataBean2 implements Serializable{
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

    private int mEulerHeading;
    private int mEulerRoll;
    private int mEulerPitch;

    private int mQuaternionW;
    private int mQuaternionX;
    private int mQuaternionY;
    private int mQuaternionZ;

    private int mPressure;

    private double mLatitude;
    private double mLongitude;
    private double mAltitude;
    private float mSpeed;

    private int mObdSpeed;
    private int mTemperature;
    private int mRpm;


    public RawDataBean2(RawDataItem iio, RawDataItem gps, RawDataItem obd) {
        if (iio != null) {
            mPts = iio.getPtsMs();
            IioData iioData = (IioData)iio.data;
            mAccX = iioData.accX;
            mAccY = iioData.accY;
            mAccZ = iioData.accZ;
            mGyroX = iioData.gyro_x;
            mGyroY = iioData.gyro_y;
            mGyroZ = iioData.gyro_z;
            mMagnX = iioData.magn_x;
            mMagnY = iioData.magn_y;
            mMagnZ = iioData.magn_z;
            mEulerHeading = iioData.euler_heading;
            mEulerRoll = iioData.euler_roll;
            mEulerPitch = iioData.euler_pitch;
            mQuaternionW = iioData.quaternion_w;
            mQuaternionX = iioData.quaternion_x;
            mQuaternionY = iioData.quaternion_y;
            mQuaternionZ = iioData.quaternion_z;
            mPressure = iioData.pressure;
        }

        if (gps != null) {
            if (mPts == 0) {
                mPts = gps.getPtsMs();
            }
            GpsData gpsData = (GpsData)gps.data;
            mLatitude = gpsData.coord.lat;
            mLongitude = gpsData.coord.lng;
            mAltitude = gpsData.altitude;
            mSpeed = gpsData.speed;
        }

        if (obd != null) {
            if (mPts == 0) {
                mPts = obd.getPtsMs();
            }

            ObdData obdData = (ObdData)obd.data;
            mObdSpeed = obdData.speed;
            mTemperature = obdData.temperature;
            mRpm = obdData.rpm;
        }
    }
}
