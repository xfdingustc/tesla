package com.waylens.hachi.bgjob.download;

import android.os.Environment;
import android.text.TextUtils;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.DownloadManager;
import com.waylens.hachi.app.UploadManager;
import com.waylens.hachi.bgjob.Exportable;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by Xiaofei on 2015/11/11.
 */
public class DownloadHelper {

    public static final String VIDEO_DOWNLOAD_PATH = "/waylens/video/Waylens/";
    public static final String PICTURE_DOWNLOAD_PATH = "/waylens/picture/Waylens/";
    public static final String MOMENT_CACHE_PATH = "/waylens/cache/";
    public static final String FW_DOWNLOAD_PATH = "/waylens/downloads/firmware/";



    public static String getVideoDownloadPath() {
        return getDownloadPath(VIDEO_DOWNLOAD_PATH);
    }

    public static String getFirmwareDownloadPath() {
        return getDownloadPath(FW_DOWNLOAD_PATH) + "firmware.tsf";
    }

    public static String getMomentCachePath() {
        return getDownloadPath(MOMENT_CACHE_PATH);
    }


    public static File[] getDownloadedFileList() {
        File downloadDir = new File(getVideoDownloadPath());
        File[] fileList = downloadDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                DownloadManager manager = DownloadManager.getManager();
                for (int i = 0; i < manager.getCount(); i++) {
                    Exportable exportable = manager.getDownloadJob(i);
                    if (!TextUtils.isEmpty(exportable.getOutputFile()) &&exportable.getOutputFile().endsWith(s)) {
                        return false;
                    }
                }

                return true;
            }
        });

        File[] decFileList = new File[fileList.length];
        for (int i = 0; i < fileList.length; i++) {
            decFileList[i] = fileList[fileList.length - i - 1];
        }

        return decFileList;
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
