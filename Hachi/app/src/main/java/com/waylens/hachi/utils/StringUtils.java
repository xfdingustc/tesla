package com.waylens.hachi.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Created by Xiaofei on 2016/9/5.
 */
public class StringUtils {
    public static String getHostName(String urlString) {
        String head = "";
        int index = urlString.indexOf("://");
        if (index != -1) {
            head = urlString.substring(0, index + 3);
            urlString = urlString.substring(index + 3);
        }
        index = urlString.indexOf("/");
        if (index != -1) {
            urlString = urlString.substring(0, index + 1);
        }
        return head + urlString;
    }


    public static String getHostNameWithoutPrefix(String urlString) {
        int index = urlString.indexOf("://");
        if (index != -1) {
            urlString = urlString.substring(index + 3);
        }
        index = urlString.indexOf("/");
        if (index != -1) {
            urlString = urlString.substring(0, index);
        }
        return urlString;
    }


    public static String getFileName(String urlString) {
        int index = urlString.lastIndexOf("/");
        if (index != -1) {
            urlString = urlString.substring(index);
        }

        return "" + urlString;
    }

    public static String getDataSize(long var0) {
        DecimalFormat var2 = new DecimalFormat("###.00");
        return var0 < 1024L ? var0 + "bytes" : (var0 < 1048576L ? var2.format((double) ((float) var0 / 1024.0F))
            + "KB" : (var0 < 1073741824L ? var2.format((double) ((float) var0 / 1024.0F / 1024.0F))
            + "MB" : (var0 < 0L ? var2.format((double) ((float) var0 / 1024.0F / 1024.0F / 1024.0F))
            + "GB" : "error")));
    }

    public static String getRaceTime(long time) {
        BigDecimal tmp = new BigDecimal((float)time / 1000f);
        return String.valueOf(tmp.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue()) + "s";

    }

    public static String getSpaceString(long space) {
        float spaceInM = ((float)space) / (1000 * 1000);

        String spaceStr;
        if (spaceInM > 1000) {
            BigDecimal tmp = new BigDecimal(spaceInM / 1000);
            spaceStr = String.valueOf(tmp.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue()) + " GB";
        } else {
            BigDecimal tmp = new BigDecimal(spaceInM);
            spaceStr = String.valueOf(tmp.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue()) + " MB";
        }
        return spaceStr;
    }

    public static String getSpaceNumber(long space) {
        float spaceInM = ((float)space) / (1000 * 1000);

        String spaceStr;
        if (spaceInM > 1000) {
            BigDecimal tmp = new BigDecimal(spaceInM / 1000);
            spaceStr = String.valueOf(tmp.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue());
        } else {
            BigDecimal tmp = new BigDecimal(spaceInM);
            spaceStr = String.valueOf(tmp.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue());
        }
        return spaceStr;
    }


    public static String getSpaceUnit(long space) {
        float spaceInM = ((float)space) / (1000 * 1000);

        String spaceStr;
        if (spaceInM > 1000) {
            spaceStr = "GB";
        } else {
            spaceStr = "MB";
        }
        return spaceStr;
    }

}
