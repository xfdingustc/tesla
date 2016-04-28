package com.waylens.hachi.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.utils.L;
import com.waylens.hachi.R;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by Richard on 8/18/15.
 */
public class ImageUtils {

    public static void initImageLoader(Context context) {
        ImageLoaderConfiguration config = new ImageLoaderConfiguration
            .Builder(context)
            .threadPoolSize(3)
            .threadPriority(Thread.NORM_PRIORITY - 2)
            .denyCacheImageMultipleSizesInMemory()
            .memoryCache(new UsingFreqLimitedMemoryCache(2 * 1024 * 1024))
            .memoryCacheSize(2 * 1024 * 1024)
            .discCacheSize(50 * 1024 * 1024)
            .discCacheFileNameGenerator(new Md5FileNameGenerator())
            .tasksProcessingOrder(QueueProcessingType.LIFO)
            .discCacheFileCount(300)
            .discCache(new UnlimitedDiskCache(getStorageDir(context, "cache-img")))
            .defaultDisplayImageOptions(DisplayImageOptions.createSimple())
            .imageDownloader(new BaseImageDownloader(context, 5 * 1000, 30 * 1000))
            .build();
        ImageLoader.getInstance().init(config);
        L.writeLogs(false);
    }

    private static BitmapFactory.Options getDefaultOptions() {
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inPreferredConfig = Bitmap.Config.RGB_565;
        return option;
    }

    public static DisplayImageOptions getAvatarOptions() {
        return new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.waylens_logo_76x86)
            .showImageForEmptyUri(R.drawable.waylens_logo_76x86)
            .showImageOnFail(R.drawable.waylens_logo_76x86)
            .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
            .decodingOptions(getDefaultOptions())
            .cacheInMemory(true)
            .cacheOnDisc(true)
            .build();
    }

    public static DisplayImageOptions getVideoOptions() {
        return new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.defaultpic)
            .showImageForEmptyUri(R.drawable.defaultpic)
            .showImageOnFail(R.drawable.defaultpic)
            .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
            .decodingOptions(getDefaultOptions())
            .cacheInMemory(true)
            .cacheOnDisc(true)
            .build();
    }

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
