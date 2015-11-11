package com.waylens.hachi.utils;

import android.content.Context;
import android.os.Environment;


import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.waylens.hachi.R;

import java.io.File;

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
                .discCache(new UnlimitedDiskCache(getImageStorageDir(context, "cache-img")))
                .defaultDisplayImageOptions(DisplayImageOptions.createSimple())
                .imageDownloader(new BaseImageDownloader(context, 5 * 1000, 30 * 1000))
                .build();
        ImageLoader.getInstance().init(config);
    }

    public static DisplayImageOptions getAvatarOptions() {
        return new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.sailor)
                .showImageForEmptyUri(R.drawable.sailor)
                .showImageOnFail(R.drawable.sailor)
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .build();
    }

    public static DisplayImageOptions getVideoOptions() {
        return new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.default_video_cover)
                .showImageForEmptyUri(R.drawable.default_video_cover)
                .showImageOnFail(R.drawable.default_video_cover)
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .build();
    }

    public static String getImageStoragePath(Context context, String type) {
        return getImageStorageDir(context, type).getPath();
    }

    public static File getImageStorageDir(Context context, String type) {
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
}
