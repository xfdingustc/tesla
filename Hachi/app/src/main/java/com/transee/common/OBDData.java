package com.transee.common;

/**
 * Created by liangyx on 7/6/15.
 */
public class OBDData {
    public int speed;
    public int temperature;
    public int rpm;

    public OBDData(int speed, int temperature, int rpm) {
        this.speed = speed;
        this.temperature = temperature;
        this.rpm = rpm;
    }

    public String toString() {
        return String.format("Speed[%d], Temperature[%d], RPM[%d]", speed, temperature, rpm);
    }
}
