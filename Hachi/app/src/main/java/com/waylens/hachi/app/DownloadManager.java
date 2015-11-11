package com.waylens.hachi.app;

import android.os.Environment;

import java.io.File;

/**
 * Created by Xiaofei on 2015/11/11.
 */
public class DownloadManager {

    public static final String VIDEO_DOWNLOAD_PATH = "/Transee/video/Vidit/";
    public static final String PICTURE_DOWNLOAD_PATH = "/Transee/picture/Vidit/";

    private static DownloadManager mSharedManager = new DownloadManager();

    public static DownloadManager getManager() {

        return mSharedManager;
    }

    private DownloadManager() {

    }



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
