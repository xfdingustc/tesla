package com.waylens.hachi.ui.entities;

/**
 * Created by Richard on 11/3/15.
 */
public class MomentAcc {
    public long captureTime;
    public int accX;
    public int accY;
    public int accZ;

    // gyro : Dps x 1000 = mDps
    public int gyroX;
    public int gyroY;
    public int gyroZ;

    // magn : uT x 1000000
    public int magnX;
    public int magnY;
    public int magnZ;

    // Orientation
    // Euler : Degrees x 1000 = mDegrees
    public int eulerHeading;
    public int eulerRoll;
    public int eulerPitch;

    // Quaternion : Raw, no unit
    public int quaternionW;
    public int quaternionX;
    public int quaternionY;
    public int quaternionZ;

    public int pressure;

    public MomentAcc(long captureTime, int accX, int accY, int accZ,
                     int gyroX, int gyroY, int gyroZ,
                     int magnX, int magnY, int magnZ,
                     int eulerHeading, int eulerRoll, int eulerPitch,
                     int quaternionW, int quaternionX, int quaternionY, int quaternionZ,
                     int pressure) {
        this.captureTime = captureTime;
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
        this.magnX = magnX;
        this.magnY = magnY;
        this.magnZ = magnZ;
        this.eulerHeading = eulerHeading;
        this.eulerRoll = eulerRoll;
        this.eulerPitch = eulerPitch;
        this.quaternionW = quaternionW;
        this.quaternionX = quaternionX;
        this.quaternionY = quaternionY;
        this.quaternionZ = quaternionZ;
        this.pressure = pressure;
    }
}
