package com.waylens.hachi.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;


public class ImageUtils {

    public static String getStoragePath(Context context, String type) {
        return getStorageDir(context, type).getPath();
    }

    public static File getStorageDir(Context context, String type) {
        if (isExternalStorageReady()) {
            return new File(context.getExternalCacheDir(), type);
        } else {
            return new File(context.getCacheDir(), type);
        }
    }

    public static boolean isExternalStorageReady() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
            && Environment.getExternalStorageDirectory().canWrite();
    }


    public static String saveBitmap(Bitmap bmp, String fullPath) {
        int lastIndex = fullPath.lastIndexOf('/');
        if (lastIndex == -1) {
            return null;
        }
        String path = fullPath.substring(0, lastIndex);
        String name = fullPath.substring(lastIndex + 1);
        saveBitmap(bmp, path, name);
        return fullPath;
    }


    public static String saveBitmap(Bitmap bmp, String path, String name) {
        // File file = new File("mnt/sdcard/picture");
        File file = new File(path);
        String fullPath = null;
        if (!file.exists()) {
            file.mkdirs();
        }
        fullPath = file.getPath() + "/" + name;
        if (new File(path + name).exists()) {
            return fullPath;
        }

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fullPath);

            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fullPath;
    }

    public static Bitmap zoomBitmap(Bitmap bitmap, int w, int h) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidht = ((float) w / width);
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidht, scaleHeight);
        Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return newbmp;
    }
}
