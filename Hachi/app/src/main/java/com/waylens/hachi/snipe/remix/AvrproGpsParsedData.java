package com.waylens.hachi.snipe.remix;


/**
 * Created by lshw on 16/12/8.
 */

public class AvrproGpsParsedData {
    public long clip_time_ms;
    public float speed;
    public double latitude;
    public double longitude;
    public double altitude;
    public float track;
    public long utc_time;
    public long utc_time_usec;
    public AvrproGpsParsedData(long clip_time_ms, float speed, double latitude, double longitude,
                               double altitude, float track, long utc_time, long utc_time_usec) {
        this.clip_time_ms = clip_time_ms;
        this.speed = speed;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.track = track;
        this.utc_time = utc_time;
        this.utc_time_usec = utc_time_usec;
    }
    public AvrproGpsParsedData(double latitude, double longitude, long utc_time, long utc_time_usec) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.utc_time = utc_time;
        this.utc_time_usec = utc_time_usec;

        this.clip_time_ms = 0;
        this.speed = 0;
        this.altitude = 0;
        this.track = 0;
    }
}
