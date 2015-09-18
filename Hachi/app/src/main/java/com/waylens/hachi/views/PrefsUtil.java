package com.waylens.hachi.views;

import android.content.Context;
import android.content.SharedPreferences;
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
 * Created by liangyx on 7/20/15.
 */
public class PrefsUtil {
    private static final String KEY_WEATHER_TEMP_F = "weather.temp.f";
    private static final String KEY_WEATHER_WIND_SPEED = "weather.wind.speed";
    private static final String KEY_WEATHER_ICON_URL = "weather.icon.url";
    private static final String KEY_WEATHER_UPDATE_TIME = "weather.update.time";
    private static final String KEY_GPS_SOURCE = "gps.src";

    private static Context mContext;
    private static SharedPreferences mPrefs;

    private static final String KEY_MODE = "data.mode";

    public static final  int GPS_DEVICE = 0;
    public static final  int GPS_CAMERA = 1;


    public static void init(Context context) {
        mContext = context;
    }

    private static SharedPreferences getPrefs() {
        if (mPrefs == null) {
            mPrefs = mContext.getSharedPreferences("demo.prefs", Context.MODE_PRIVATE);
        }
        return mPrefs;
    }

    public static void setDataMode(int mode) {
        getPrefs().edit().putInt(KEY_MODE, mode).apply();
    }

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

    public static DisplayImageOptions getOptions(){
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.map_weather_cloudy_icon)
                .showImageForEmptyUri(R.drawable.map_weather_cloudy_icon)
                .showImageOnFail(R.drawable.map_weather_cloudy_icon)
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

    public static void setWeatherTempF(String tempF) {
        getPrefs().edit().putString(KEY_WEATHER_TEMP_F, tempF).apply();
    }

    public static void setWeatherWindSpeed(String windspeedKmph) {
        getPrefs().edit().putString(KEY_WEATHER_WIND_SPEED, windspeedKmph).apply();
    }

    public static void setWeatherIcon(String iconUrl) {
        getPrefs().edit().putString(KEY_WEATHER_ICON_URL, iconUrl).apply();
    }

    public static String getWeatherTempF() {
        return  getPrefs().getString(KEY_WEATHER_TEMP_F, null);
    }

    public static String getWeatherWindSpeed() {
        return getPrefs().getString(KEY_WEATHER_WIND_SPEED, null);
    }

    public static String getWeatherIcon() {
        return getPrefs().getString(KEY_WEATHER_ICON_URL, null);
    }

    public static  void setUpdateWeatherTime(long time) {
        getPrefs().edit().putLong(KEY_WEATHER_UPDATE_TIME, time).apply();
    }

    public static long getUpdateWeatherTime() {
        return getPrefs().getLong(KEY_WEATHER_UPDATE_TIME, 0);
    }

    public static void setGPSSource(int gpsSource) {
        getPrefs().edit().putInt(KEY_GPS_SOURCE, gpsSource).apply();
    }

    public static int getGPSource() {
        return getPrefs().getInt(KEY_GPS_SOURCE, GPS_DEVICE);
    }
}
