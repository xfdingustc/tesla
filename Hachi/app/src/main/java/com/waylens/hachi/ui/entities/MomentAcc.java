package com.waylens.hachi.ui.entities;

/**
 * Created by Richard on 11/3/15.
 */
public class MomentAcc {
    public long captureTime;
    public int accX;
    public int accY;
    public int accZ;

    public MomentAcc(long captureTime, int x, int y, int z) {
        this.captureTime = captureTime;
        accX = x;
        accY = y;
        accZ = z;
    }
}
