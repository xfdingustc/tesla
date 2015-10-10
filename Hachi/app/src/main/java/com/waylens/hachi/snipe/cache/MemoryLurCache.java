package com.waylens.hachi.snipe.cache;

import android.graphics.Bitmap;
import android.util.LruCache;

/**
 * Memory cache impl
 * Created by Richard on 10/8/15.
 */
public class MemoryLurCache extends LruCache<String, Bitmap> {

    /**
     * Init MemoryLurCache
     * @param maxSize - size in KB
     */
    public MemoryLurCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getByteCount() / 1024;
    }
}
