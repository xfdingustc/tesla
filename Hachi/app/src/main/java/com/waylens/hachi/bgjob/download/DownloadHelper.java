package com.waylens.hachi.bgjob.download;

import android.os.Environment;

import java.io.File;

/**
 * Created by Xiaofei on 2015/11/11.
 */
public class DownloadHelper {

    public static final String VIDEO_DOWNLOAD_PATH = "/waylens/video/Vidit/";
    public static final String PICTURE_DOWNLOAD_PATH = "/waylens/picture/Vidit/";




    public static String getVideoDownloadPath() {
        return getDownloadPath(VIDEO_DOWNLOAD_PATH);
    }

    public static String getPicturePath() {
        return getDownloadPath(PICTURE_DOWNLOAD_PATH);
    }


    private static final String getDownloadPath(String subdir) {
        File sdCardDir = Environment.getExternalStorageDirectory();
        if (sdCardDir == null) {
            return null;
        }
        String dir = sdCardDir.toString() + subdir;
        File dirFile = new File(dir);
        dirFile.mkdirs();
        return dir;
    }
}
