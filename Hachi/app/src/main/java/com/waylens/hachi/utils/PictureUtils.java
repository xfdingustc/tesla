package com.waylens.hachi.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import com.orhanobut.logger.Logger;

import java.io.IOException;

/**
 * Created by lshw on 16/11/4.
 */

public class PictureUtils {
    public static String TAG = PictureUtils.class.getSimpleName();

    public static Bitmap extractPicture(String srcImgPath) {
        return extractPicture(srcImgPath, 1.0);
    }
    public static Bitmap extractPicture(String srcImgPath, double ratio) {
        Bitmap bitmap = BitmapFactory.decodeFile(srcImgPath);
        Bitmap bmp;
        int orientation = -1;
        try {
            ExifInterface exif = new ExifInterface(srcImgPath);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            Logger.t(TAG).d("orientation:" + orientation);
        } catch (IOException e) {
            Logger.t(TAG).d(e.getMessage());
        }
        Matrix m = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                m.postRotate(90);
                bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                m.postRotate(180);
                bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                m.postRotate(270);
                bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                break;
            default:
                bmp = bitmap;
                break;
        }
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        if (ratio < 1.0f) {
            ratio = 1;
        }
        int newW = (int) (w / ratio);
        int newH = (int) (h / ratio);
        if (ratio > 1.0f) {
            bmp = ImageUtils.zoomBitmap(bmp, newW, newH);
        }
        return bmp;
    }
}
