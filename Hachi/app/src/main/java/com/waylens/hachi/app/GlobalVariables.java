package com.waylens.hachi.app;

import android.net.Uri;
import android.os.Environment;

import com.googlecode.javacv.cpp.opencv_contrib;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Created by Xiaofei on 2016/4/26.
 */
public class GlobalVariables {
    private static final File OUTPUT_DIR = Environment.getExternalStorageDirectory();

    private static final String OUTPUT_PATH = OUTPUT_DIR.toString() + "/DCIM/Camera/";

    public static Uri getPictureUri() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
        String date = sDateFormat.format(new java.util.Date());
        try {
            File image = File.createTempFile(date, ".jpg", Environment.getExternalStorageDirectory());
            return Uri.fromFile(image);
        } catch (IOException e) {
            Logger.t(GlobalVariables.class.getSimpleName()).d(e.getMessage());
        }
        return null;
    }

    public static String getAvatarUrl() {
        return Environment.getExternalStorageDirectory() + "/avatar/Images";
    }
}
