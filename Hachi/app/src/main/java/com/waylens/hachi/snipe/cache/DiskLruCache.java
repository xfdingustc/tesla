package com.waylens.hachi.snipe.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;

import com.waylens.hachi.utils.ImageUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Disk cache impl
 * Created by Richard on 10/8/15.
 */
public class DiskLruCache {

    File cacheDir;
    FileLruCache mFileLruCache;

    private volatile static DiskLruCache instance;

    /**
     * Create an instance of DiskLruCache
     *
     * @param maxSize - size in KB
     */
    protected DiskLruCache(Context context, int maxSize) {
        mFileLruCache = new FileLruCache(maxSize);
        cacheDir = ImageUtils.getImageStorageDir(context, "diskCache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    public void init() {
        File[] files = cacheDir.listFiles();
        for (File file : files) {
            mFileLruCache.put(file.getName(), (int) file.length() / 1024);
        }
    }


    public static DiskLruCache getDiskLruCache(Context context, int maxSize) {
        if (instance == null) {
            synchronized (DiskLruCache.class) {
                if (instance == null) {
                    instance = new DiskLruCache(context, maxSize);
                }
            }
        }
        return instance;
    }

    public synchronized void put(String key, Bitmap bitmap) {
        int fileSize = cacheToDisk(key, bitmap);
        mFileLruCache.put(key, fileSize);
    }

    public synchronized Bitmap get(String key) {
        Integer size = mFileLruCache.get(key);
        if (size == null) {
            return null;
        } else {
            return BitmapFactory.decodeFile(cacheDir + File.separator + key);
        }
    }

    private int cacheToDisk(String key, Bitmap bitmap) {
        File file = new File(cacheDir, key);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (FileNotFoundException e) {
            Log.e("test", "", e);
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    Log.e("test", "", e);
                }
            }
        }
        return (int) file.length() / 1024;
    }

    public void evictAll() {
        mFileLruCache.evictAll();
    }

    class FileLruCache extends LruCache<String, Integer> {

        public FileLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, Integer value) {
            return value;
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Integer oldValue, Integer newValue) {
            if (evicted) {
                new File(cacheDir, key).delete();
                Log.e("test", "Delete: " + key);
            }
            super.entryRemoved(evicted, key, oldValue, newValue);
        }
    }

}
