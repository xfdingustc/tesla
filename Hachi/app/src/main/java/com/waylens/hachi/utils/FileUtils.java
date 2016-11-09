package com.waylens.hachi.utils;

import android.os.Environment;
import android.text.TextUtils;

import com.waylens.hachi.bgjob.export.ExportManager;
import com.waylens.hachi.bgjob.export.ExportableJob;
import com.waylens.hachi.snipe.utils.DateTime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Xiaofei on 2016/9/2.
 */
public class FileUtils {
    public static final String VIDEO_DOWNLOAD_PATH = "/waylens/video/Waylens/";
    public static final String PICTURE_DOWNLOAD_PATH = "/waylens/picture/Waylens/";
    public static final String MOMENT_CACHE_PATH = "/waylens/cache/";
    public static final String FW_DOWNLOAD_PATH = "/waylens/downloads/firmware/";

    public static String getVideoExportPath() {
        return getExportPath(VIDEO_DOWNLOAD_PATH);
    }

    public static String getFirmwareDownloadPath() {
        return getExportPath(FW_DOWNLOAD_PATH) + "firmware.tsf";
    }

    public static String getMomentCachePath() {
        return getExportPath(MOMENT_CACHE_PATH);
    }


    public static File[] getExportedFileList() {
        File downloadDir = new File(getVideoExportPath());
        File[] fileList = downloadDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                ExportManager manager = ExportManager.getManager();
                for (int i = 0; i < manager.getCount(); i++) {
                    ExportableJob exportable = manager.getDownloadJob(i);
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


    private static String getExportPath(String subdir) {
        File sdCardDir = Environment.getExternalStorageDirectory();
        if (sdCardDir == null) {
            return null;
        }
        String dir = sdCardDir.toString() + subdir;
        File dirFile = new File(dir);
        dirFile.mkdirs();
        return dir;
    }


    private static String composeFileName(String dir, String fn, int i) {
        if (i == 0) {
            return dir + fn + ".mp4";
        } else {
            return dir + fn + "-" + Integer.toString(i) + ".mp4";
        }
    }


    public static String genDownloadFileName(int clipDate, long clipTimeMs) {
        try {
            String dir = getVideoExportPath();
            if (dir == null) {
                return null;
            }
            String fn = DateTime.toFileName(clipDate, clipTimeMs);
            for (int i = 0; ; i++) {
                String targetFile = composeFileName(dir, fn, i);
                File file = new File(targetFile);
                if (!file.exists()) {
                    return targetFile;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String genMomentCacheFileName(String fileName) {
        String dir = getMomentCachePath();
        if (dir == null) {
            return null;
        }

        for (int i = 0; ; i++) {
            String targetFile = composeFileName(dir, fileName, i);
            File file = new File(targetFile);
            if (!file.exists()) {
                return targetFile;
            }
        }
    }


    public static void writeFile(InputStream inputStream, File file) throws IOException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        if (file != null && file.exists()) {
            file.delete();
        }

        FileOutputStream out = new FileOutputStream(file);
        byte[] buffer = new byte[1024 * 128];
        int length = -1;
        while ((length = inputStream.read(buffer)) != -1 ) {
            out.write(buffer, 0, length);
        }

        out.flush();
        out.close();
        inputStream.close();
    }
}
