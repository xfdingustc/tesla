package com.waylens.hachi.bgjob.download;

import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by Xiaofei on 2015/11/11.
 */
public class DownloadHelper {

    public static final String VIDEO_DOWNLOAD_PATH = "/waylens/video/Vidit/";
    public static final String PICTURE_DOWNLOAD_PATH = "/waylens/picture/Vidit/";


    public static String getVideoDownloadPath() {
        return getDownloadPath(VIDEO_DOWNLOAD_PATH);
    }

    public static File[] getDownloadedFileList() {
        File downloadDir = new File(getVideoDownloadPath());
        File[] fileList = downloadDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                if (s.endsWith(".mp4")) {
                    return true;
                }
                return false;
            }
        });

        File[] decFileList = new File[fileList.length];
        for (int i = 0; i < fileList.length; i++) {
            decFileList[i] = fileList[fileList.length - i - 1];
        }

        return decFileList;
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
