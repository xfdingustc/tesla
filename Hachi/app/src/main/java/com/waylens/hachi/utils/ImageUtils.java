package com.waylens.hachi.utils;

import android.content.Context;
import android.os.Environment;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
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
                .discCache(new UnlimitedDiscCache(new File(getImageStoragePath(context))))
                .defaultDisplayImageOptions(DisplayImageOptions.createSimple())
                .imageDownloader(new BaseImageDownloader(context, 5 * 1000, 30 * 1000))
                .build();
        ImageLoader.getInstance().init(config);
    }

    public static DisplayImageOptions getAvatarOptions(){
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.sailor)
                .showImageForEmptyUri(R.drawable.sailor)
                .showImageOnFail(R.drawable.sailor)
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .build();
        return options;
    }

    public static String getImageStoragePath(Context context){
        String dir = "/th";
        String path = getDefaultStoragePath(context) + dir;
        return path;
    }

    public static String getDefaultStoragePath(Context context){
        if(Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED) && Environment.getExternalStorageDirectory().canWrite()){
            return Environment.getExternalStorageDirectory().getPath();
        }else{
            return context.getFilesDir().getPath();
        }
    }

}
