package com.waylens.hachi.utils;

/**
 * Created by Xiaofei on 2015/5/8.
 */
public class DigitUtils {
    public static short hBytes2Short(byte[] data) {
        short ret = 0;
        if (data[0] >= 0) {
            ret += data[0];
        } else {
            ret += 256 + data[0];
        }
        ret = (short) (ret * 256);

        if (data[1] >= 0) {
            ret += data[1];
        } else {
            ret += 256 + data[1];
        }

        return ret;
    }
}
