package com.waylens.hachi.utils;

import android.util.Log;

import java.security.MessageDigest;

/**
 * Created by Xiaofei on 2015/5/8.
 */
public class DigitUtils {

    private static MessageDigest md5Digest;

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

    private static char[] hextable = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String md5(final String toEncrypt) {
        try {
            if (md5Digest == null) {
                md5Digest = MessageDigest.getInstance("md5");
            }
            md5Digest.update(toEncrypt.getBytes());
            final byte[] bytes = md5Digest.digest();
            final StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                int tmp = 0xFF & b;
                sb.append(hextable[tmp >> 4 & 0xF]).append(hextable[tmp & 0xF]);
            }
            return sb.toString().toLowerCase();
        } catch (Exception exc) {
            return null;
        }
    }
}
