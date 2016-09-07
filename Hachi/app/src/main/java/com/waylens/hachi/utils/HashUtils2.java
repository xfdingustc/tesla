package com.waylens.hachi.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Xiaofei on 2016/9/8.
 */
public abstract class HashUtils2 {
    private static final String TAG = HashUtils2.class.getSimpleName();

    private static MessageDigest sha1Inst;
    private static MessageDigest md5Inst;

    public static byte[] encodeMD5(byte[] bytes) throws NoSuchAlgorithmException {

        byte[] btInput = bytes;
        if (md5Inst == null) {
            md5Inst = MessageDigest.getInstance("MD5");
        }

        md5Inst.update(btInput);

        return md5Inst.digest();

    }

    public static byte[] encodeMD5(String s) throws NoSuchAlgorithmException {
        return encodeMD5(s.getBytes());

    }

    public static byte[] encodeMD5(File file) throws IOException, NoSuchAlgorithmException {
        FileInputStream in = new FileInputStream(file);
        MessageDigest sha1Inst = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[1024 * 4];
        int len = 0;


        while ((len = in.read(buffer)) > 0) {
            sha1Inst.update(buffer, 0, len);
        }
        byte[] ret = sha1Inst.digest();
        return ret;
    }


    public static byte[] encodeSHA1(File file) throws IOException, NoSuchAlgorithmException {

        FileInputStream in = new FileInputStream(file);
        MessageDigest sha1Inst = MessageDigest.getInstance("SHA-1");
        byte[] buffer = new byte[1024 * 4];
        int len = 0;
        int cnt = 0;


        while ((len = in.read(buffer)) > 0) {
            sha1Inst.update(buffer, 0, len);
        }
        byte[] ret = sha1Inst.digest();
        return ret;


    }

    public static byte[] SHA1(File file, byte[] header, byte[] tail) throws IOException, NoSuchAlgorithmException {
        FileInputStream in = new FileInputStream(file);
        MessageDigest sha1Inst = MessageDigest.getInstance("SHA-1");
        if (header != null) {
            sha1Inst.update(header);
        }
        byte[] buffer = new byte[1024 * 4];
        int len = 0;
        int cnt = 0;

        while ((len = in.read(buffer)) > 0) {
            sha1Inst.update(buffer, 0, len);
        }
        if (tail != null) {
            sha1Inst.update(tail);
        }
        return sha1Inst.digest();
    }

    public static byte[] SHA1(byte[] data, int length) throws NoSuchAlgorithmException {
        if (sha1Inst == null) {
            sha1Inst = MessageDigest.getInstance("SHA-1");
        }
        sha1Inst.update(data, 0, length);
        return sha1Inst.digest();
    }


    public static byte[] encodeHMAC256(String key, String data) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        return sha256_HMAC.doFinal(data.getBytes("UTF-8"));
    }
}
