package com.waylens.hachi.vdb;

import android.util.Log;

import com.transee.common.Utils;

/**
 * Created by liangyx on 7/6/15.
 */
public class OBDData {
    public int speed;
    public int temperature;
    public int rpm;

    public static final int OFF_revision = 0;
    public static final int OFF_total_size = 4;
    public static final int OFF_pid_info_size = 8;
    public static final int OFF_pid_data_size = 12;
    public static final int OFF_HEAD = 40;


    public static final int INDEX_TEMP = 0x05;
    public static final int INDEX_RPM = 0x0C;
    public static final int INDEX_SPEED = 0x0D;


    public OBDData(int speed, int temperature, int rpm) {
        this.speed = speed;
        this.temperature = temperature;
        this.rpm = rpm;
    }

    public String toString() {
        return String.format("Speed[%d], Temperature[%d], RPM[%d]", speed, temperature, rpm);
    }



    public static OBDData parse(byte[] data) {
        if (data == null || data.length < 40) {
            Log.e("OBDData", "Invalid OBD data.");
            return null;
        }

        int revisionCode = Utils.readi32(data, OFF_revision);
        int totalSize = Utils.readi32(data, OFF_total_size);
        int pidInfoSize = Utils.readi32(data, OFF_pid_info_size);
        int pidDataSize = Utils.readi32(data, OFF_pid_data_size);
        int INDEX_INFO_START = OFF_HEAD;
        int INDEX_DATA_START = OFF_HEAD + pidInfoSize;

        int flag = Utils.read16(data, INDEX_INFO_START + INDEX_TEMP * 4);
        int temperature = 0;
        if ((flag & 0x1) == 1) {
            int offsetTemp = Utils.read16(data, INDEX_INFO_START + INDEX_TEMP * 4 + 2);
            temperature = (data[INDEX_DATA_START + offsetTemp] & 0x00FF) - 40;
        }
        flag = Utils.read16(data, INDEX_INFO_START + INDEX_SPEED * 4);
        int speed = 0;
        if ((flag & 0x1) == 1) {
            int offsetSpeed = Utils.read16(data, INDEX_INFO_START + INDEX_SPEED * 4 + 2);
            speed = data[INDEX_DATA_START + offsetSpeed] & 0x00FF;
        }

        int rpm = 0;
        flag = Utils.read16(data, INDEX_INFO_START + INDEX_RPM * 4);
        if ((flag & 0x1) == 1) {
            int offsetRMP = Utils.read16(data, INDEX_INFO_START + INDEX_RPM * 4 + 2);
            rpm = data[INDEX_DATA_START + offsetRMP] & 0x000000FF;
            rpm <<= 8;
            rpm |= (data[INDEX_DATA_START + offsetRMP + 1] & 0x000000FF);
            rpm >>= 2;
        }

        return new OBDData(speed, temperature, rpm);
    }
}
