package com.waylens.hachi.vdb;

import android.util.Log;

import com.transee.common.Utils;
import com.waylens.hachi.utils.ViewUtils;

/**
 * Created by Xiaofei on 2015/9/14.
 */
public class AccData {
    //// accel : g x 1000 = mg
    public int accX;
    public int accY;
    public int accZ;

    //---------------------------------------------------
    public int version;   // IIO_VERSION
    int size;      // sizeof(iio_raw_data_s)
    public int flags;     // IIO_F_ACCEL etc
    //---------------------------------------------------

    // gyro : Dps x 1000 = mDps
    public int gyro_x;
    public int gyro_y;
    public int gyro_z;

    // magn : uT x 1000000
    public int magn_x;
    public int magn_y;
    public int magn_z;

    // Orientation
    // Euler : Degrees x 1000 = mDegrees
    public int euler_heading;
    public int euler_roll;
    public int euler_pitch;

    // Quaternion : Raw, no unit
    public int quaternion_w;
    public int quaternion_x;
    public int quaternion_y;
    public int quaternion_z;

    // Pressure: Pa x 1000
    public int pressure;

    public static final int ACC_DATA_LENGTH_V0 = 12;
    private int mPos;

    @Override
    public String toString() {
        return String.format("AccX[%d], AccY[%d], AccZ[%d]", accX, accY, accZ);
    }

    public static AccData parse(byte[] data) {
        AccData accData = new AccData();
        accData.parseData(data);
        return accData;
    }

    void parseData(byte[] data) {
        mPos = 0;
        accX = readi32(data);
        accY = readi32(data);
        accZ = readi32(data);

        if (data.length == ACC_DATA_LENGTH_V0) {
            return;
        }

        version = readi16(data);
        size = readi16(data);
        if (size != data.length) {
            version = 0;
            return;
        }
        flags = readi32(data);

        gyro_x = readi32(data);
        gyro_y = readi32(data);
        gyro_z = readi32(data);

        magn_x = readi32(data);
        magn_y = readi32(data);
        magn_z = readi32(data);

        euler_heading = readi32(data);
        euler_roll = readi32(data);
        euler_pitch = readi32(data);

        quaternion_w = readi32(data);
        quaternion_x = readi32(data);
        quaternion_y = readi32(data);
        quaternion_z = readi32(data);

        pressure = readi32(data);
    }

    int readi32(byte[] data) {
        int result = (int) data[mPos] & 0xFF;
        result |= ((int) data[mPos + 1] & 0xFF) << 8;
        result |= ((int) data[mPos + 2] & 0xFF) << 16;
        result |= ((int) data[mPos + 3] & 0xFF) << 24;
        mPos += 4;
        return result;
    }

    int readi16(byte[] data) {
        int result = (int) data[mPos] & 0xFF;
        result |= ((int) data[mPos + 1] & 0xFF) << 8;
        mPos += 2;
        return result;
    }
}
