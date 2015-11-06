package com.waylens.hachi.ui.entities;

/**
 * Created by Richard on 11/6/15.
 */
public class MomentGPS {
    public long captureTime;

    public double longitude;

    public double latitude;

    public double altitude;

    public MomentGPS(long captureTime, double longitude, double latitude, double altitude) {
        this.captureTime = captureTime;
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
    }

}
