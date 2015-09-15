package com.waylens.hachi.vdb;

import com.transee.common.Utils;

/**
 * Created by Xiaofei on 2015/9/14.
 */
public class AccData {
    public int accX;
    public int accY;
    public int accZ;

    public static final int OFFSET_ACCX = 0;
    public static final int OFFSET_ACCY = 4;
    public static final int OFFSET_ACCZ = 8;
    public static final int OFFSET_ = 12;

    public AccData(int accX, int accY, int accZ) {
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
    }

    @Override
    public String toString() {
        return String.format("AccX[%d], AccY[%d], AccZ[%d]", accX, accY, accZ);
    }

    public static AccData parse(byte[] data) {

        int accX = Utils.readi32(data, OFFSET_ACCX);
        int accY = Utils.readi32(data, OFFSET_ACCY);
        int accZ = Utils.readi32(data, OFFSET_ACCZ);
        //int pidDataSize = Utils.readi32(data, OFF_pid_data_size);
        return new AccData(accX, accY, accZ);
    }
}
