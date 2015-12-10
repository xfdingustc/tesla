package com.waylens.hachi.vdb;

import android.util.Log;

import com.transee.common.Utils;

/**
 * Created by liangyx on 7/6/15.
 */
public class OBDData {
    public static final int OBD_VERSION_1 = 1;
    public static final int OBD_VERSION_2 = 2;

    public int speed;
    public int temperature;
    public int rpm;

    public static final int OFF_revision = 0;
    public static final int OFF_total_size = 4;
    public static final int OFF_pid_info_size = 8;
    public static final int OFF_pid_data_size = 12;
    public static final int OFF_HEAD = 40;


    public static final int PID_TEMP = 0x05;        //temperature
    public static final int PID_RPM = 0x0C;         //RPM
    public static final int PID_SPEED = 0x0D;       //Speed
    public static final int PID_THROTTLE = 0x11;    // Throttle Position
    public static final int PID_BP = 0x33;          // - barometric pressure
    public static final int PID_IMP = 0x0B;         // - intake manifold absolute pressure
    public static final int PID_P_TORQUE = 0x62;    // - Actual engine percent torque
    public static final int PID_R_TORQUE = 0x63;    // - Engine reference torque

    private static final int[] g_pid_data_size_table = new int[]{
            4, 4, 2, 2, 1, 1, 2, 2,        // 00 - 07
            2, 2, 1, 1, 2, 1, 1, 1,        // 08 - 0F

            2, 1, 1, 1, 2, 2, 2, 2,        // 10 - 17
            2, 2, 2, 2, 1, 1, 1, 2,        // 18 - 1F

            4, 2, 2, 2, 4, 4, 4, 4,        // 20 - 27
            4, 4, 4, 4, 1, 1, 1, 1,        // 28 - 2F

            1, 2, 2, 1, 4, 4, 4, 4,        // 30 - 37
            4, 4, 4, 4, 2, 2, 2, 2,        // 38 - 3F

            4, 4, 2, 2, 2, 1, 1, 1,        // 40 - 47
            1, 1, 1, 1, 1, 2, 2, 4,        // 48 - 4F

            4, 1, 1, 2, 2, 2, 2, 2,        // 50 - 57
            2, 2, 1, 1, 1, 2, 2, 1,        // 58 - 5F

            4, 1, 1, 2, 5, 2, 5, 3,        // 60 - 67
            7, 7, 5, 5, 5, 6, 5, 3,        // 68 - 6F

            9, 5, 5, 5, 5, 7, 7, 5,        // 70 - 77
            9, 9, 7, 7, 9, 1, 1, 13,       // 78 - 7F
    };


    public OBDData(int speed, int temperature, int rpm) {
        this.speed = speed;
        this.temperature = temperature;
        this.rpm = rpm;
    }

    public String toString() {
        return String.format("Speed[%d], Temperature[%d], RPM[%d]", speed, temperature, rpm);
    }


    public static OBDData parse(byte[] data) {
        if (data == null) {
            Log.e("OBDData", "Invalid OBD data.");
            return null;
        }

        int revisionCode = Utils.readi32(data, OFF_revision);
        if (revisionCode == OBD_VERSION_1) {
            return parseVersion1(data);
        } else {
            return parseVersion2(data);
        }
    }

    private static OBDData parseVersion2(byte[] data) {
        int speed = 0;
        int rpm = 0;
        int temperature = 0;
        int index = 1;
        while (index < data.length) {
            int pid = data[index];
            if (pid == 0) {
                break;
            }
            index++;
            int len = g_pid_data_size_table[pid];
            switch (pid) {
                case PID_SPEED:
                    speed = data[index] & 0x00FF;
                    break;
                case PID_TEMP:
                    temperature = data[index] - 40;
                    break;
                case PID_RPM:
                    rpm = data[index] & 0x000000FF;
                    rpm <<= 8;
                    rpm |= (data[index + 1] & 0x000000FF);
                    rpm >>= 2;
                    break;
                default:
                    //Log.e("test", "PID is not supported yet: " + pid);
                    break;
            }
            index += len;
        }
        Log.e("test", String.format("speed[%d], t[%d], rpm[%d]", speed, temperature, rpm));
        return new OBDData(speed, temperature, rpm);
    }

    private static OBDData parseVersion1(byte[] data) {
        int totalSize = Utils.readi32(data, OFF_total_size);
        int pidInfoSize = Utils.readi32(data, OFF_pid_info_size);
        int pidDataSize = Utils.readi32(data, OFF_pid_data_size);

        int INDEX_INFO_START = OFF_HEAD;
        int INDEX_DATA_START = OFF_HEAD + pidInfoSize;

        int flag = Utils.read16(data, INDEX_INFO_START + PID_TEMP * 4);
        int temperature = 0;
        if ((flag & 0x1) == 1) {
            int offsetTemp = Utils.read16(data, INDEX_INFO_START + PID_TEMP * 4 + 2);
            temperature = (data[INDEX_DATA_START + offsetTemp] & 0x00FF) - 40;
        }
        flag = Utils.read16(data, INDEX_INFO_START + PID_SPEED * 4);
        int speed = 0;
        if ((flag & 0x1) == 1) {
            int offsetSpeed = Utils.read16(data, INDEX_INFO_START + PID_SPEED * 4 + 2);
            speed = data[INDEX_DATA_START + offsetSpeed] & 0x00FF;
        }

        int rpm = 0;
        flag = Utils.read16(data, INDEX_INFO_START + PID_RPM * 4);
        if ((flag & 0x1) == 1) {
            int offsetRMP = Utils.read16(data, INDEX_INFO_START + PID_RPM * 4 + 2);
            rpm = data[INDEX_DATA_START + offsetRMP] & 0x000000FF;
            rpm <<= 8;
            rpm |= (data[INDEX_DATA_START + offsetRMP + 1] & 0x000000FF);
            rpm >>= 2;
        }
        return new OBDData(speed, temperature, rpm);
    }
}
