package com.waylens.hachi.bgjob.upload;

import android.util.Log;

import com.waylens.hachi.utils.HashUtils;
import com.waylens.hachi.utils.Hex;

import java.security.NoSuchAlgorithmException;


/**
 * Created by Xiaofei on 2016/9/7.
 */
public class HachiAuthorizationHelper {
    private static final String TAG = HachiAuthorizationHelper.class.getSimpleName();


    public static String getAuthoriztion(String host, String userId, String momentId, String content, String date, String privateKey) {
        try {
            String checkSum = computeCheckSum(host, userId, momentId, date);
            Log.d(TAG, "checkSum = " + checkSum);
            String stringToSign = "WAYLENS-HMAC-SHA256&waylens_cfs&" + content + "&" + checkSum;
            String signingKey = Hex.encodeHexString(HashUtils.encodeHMAC256(privateKey, "waylens_cfs&" + date));
            Log.d(TAG, "signingKey: " + signingKey);

            String signature = Hex.encodeHexString(HashUtils.encodeHMAC256(signingKey, stringToSign));
            Log.d(TAG, "signature: " + signature);
            return "WAYLENS-HMAC-SHA256 " + signature;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getAuthoriztion(String host, String userId, long momentId, String fileSha1, String content, String date, String privateKey) {
        try {
            String checkSum = computeCheckSum(host, userId, Long.toString(momentId), fileSha1, date);
            Log.d(TAG, "checkSum = " + checkSum);
            String stringToSign = "WAYLENS-HMAC-SHA256&waylens_cfs&" + content + "&" + checkSum;
            String signingKey = Hex.encodeHexString(HashUtils.encodeHMAC256(privateKey, "waylens_cfs&" + date));
            Log.d(TAG, "signingKey: " + signingKey);

            String signature = Hex.encodeHexString(HashUtils.encodeHMAC256(signingKey, stringToSign));
            Log.d(TAG, "signature: " + signature);
            return "WAYLENS-HMAC-SHA256 " + signature;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static String getAuthoriztion(String host, String userId, long momentId, String date, String privateKey) {
        return getAuthoriztion(host, userId, Long.toString(momentId), "upload_videos", date, privateKey);
    }


    private static String computeCheckSum(String host, String userId, String momentId, String date) throws NoSuchAlgorithmException {
        String sum = host + userId + momentId + date;
        Log.d(TAG, "sum = " + sum);
        byte[] sumBytes = sum.getBytes();

        byte[] newSumByte = new byte[sumBytes.length];
        for (int i = 0; i < sumBytes.length; i++) {
            newSumByte[i] = (byte) (((int) sumBytes[i] * 7) % 256);
        }


        return Hex.encodeHexString(HashUtils.encodeMD5(newSumByte));
    }

    private static String computeCheckSum(String host, String userId, String momentId, String fileSha1, String date) throws NoSuchAlgorithmException {
        String sum = host + userId + momentId + fileSha1 + date;
        Log.d(TAG, "sum = " + sum);
        byte[] sumBytes = sum.getBytes();

        byte[] newSumByte = new byte[sumBytes.length];
        for (int i = 0; i < sumBytes.length; i++) {
            newSumByte[i] = (byte) (((int) sumBytes[i] * 7) % 256);
        }


        return Hex.encodeHexString(HashUtils.encodeMD5(newSumByte));
    }
}