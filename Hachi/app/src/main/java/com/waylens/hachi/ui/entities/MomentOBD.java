package com.waylens.hachi.ui.entities;

/**
 * Created by Richard on 11/3/15.
 */
public class MomentOBD {
    
    public long captureTime;
    public int speed;
    public int rpm;
    public int temperature;
    public int tp;
    public int imp;
    public int bp;
    public int bhp;

    public MomentOBD(long captureTime, int speed, int rpm, int temperature, int tp, int imp, int bp, int bhp) {
        this.captureTime = captureTime;
        this.speed = speed;
        this.rpm = rpm;
        this.temperature = temperature;
        this.tp = tp;
        this.imp = imp;
        this.bp = bp;
        this.bhp = bhp;
    }
}
