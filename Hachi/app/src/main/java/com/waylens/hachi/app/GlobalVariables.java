package com.waylens.hachi.app;

import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 * Created by Xiaofei on 2016/4/26.
 */
public class GlobalVariables {
    private static final File OUTPUT_DIR = Environment.getExternalStorageDirectory();

    private static final String OUTPUT_PATH = OUTPUT_DIR.toString() + "/DCIM/Camera/";

    public static String getPictureName() {
        String picture_name = new String(OUTPUT_PATH);
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
        String date = sDateFormat.format(new java.util.Date());
        picture_name += date;
        picture_name += ".jpg";
        return picture_name;
    }
}
