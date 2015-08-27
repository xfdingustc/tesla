package com.waylens.hachi.utils;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Xiaofei on 2015/3/18.
 */
public class HashUtils {
    private static final String TAG = HashUtils.class.getSimpleName();

    public final static byte[] MD5(String s) {

        try {
            byte[] btInput = s.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文

            return mdInst.digest();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public final static String MD5String(String s) {
        return getStringFromDigest(MD5(s));
    }

    private final static String getStringFromDigest(byte[] digest) {
        byte[] md = digest;
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        // 把密文转换成十六进制的字符串形式
        int j = md.length;
        char str[] = new char[j * 2];
        int k = 0;
        for (int i = 0; i < j; i++) {
            byte byte0 = md[i];
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }

        return new String(str);
    }

    public final static byte[] SHA1(File file) {
        try {
            FileInputStream in = new FileInputStream(file);
            MessageDigest sha1Inst = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[1024 * 1024 * 10];
            int len = 0;
            int cnt = 0;
            Log.i(TAG, "file length = " + file.length());

            while ((len = in.read(buffer)) > 0) {
                sha1Inst.update(buffer, 0, len);
                //sha1Inst.update(buffer);
                Log.i(TAG, "time = " + cnt++ + " len = " + len);
            }
            return sha1Inst.digest();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public final static byte[] SHA1(byte[] data, int length) {
        try {
            MessageDigest sha1Inst = MessageDigest.getInstance("SHA-1");
            sha1Inst.update(data, 0, length);
            return sha1Inst.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public final static String SHA1String(File file) {
        return getStringFromDigest(SHA1(file));
    }


}