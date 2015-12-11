package com.waylens.hachi.snipe;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.waylens.hachi.snipe.cache.DiskLruCache;
import com.waylens.hachi.snipe.cache.MemoryLurCache;
import com.waylens.hachi.snipe.toolbox.VdbImageRequest;
import com.waylens.hachi.utils.DigitUtils;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.vdb.ClipPos;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Xiaofei on 2015/8/25.
 */
public class VdbImageLoader {
    private static final String TAG = VdbImageLoader.class.getSimpleName();
    private final VdbRequestQueue mRequestQueue;
    private final int mBatchedResponsesDelayMs = 100;

    private final HashMap<String, BatchedVdbImageRequest> mInFlightRequest = new HashMap<>();

    private final ConcurrentHashMap<String, BatchedVdbImageRequest> mBatchedResponses = new ConcurrentHashMap<>();

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private Runnable mRunnable;

    static MemoryLurCache memoryLurCache;

    static DiskLruCache diskLruCache;
    private boolean mUseCache;

    public VdbImageLoader(VdbRequestQueue queue) {
        this.mRequestQueue = queue;
        if (memoryLurCache == null) {
            int size = (int) Runtime.getRuntime().maxMemory() / 1024;
            Log.e(TAG, "size: " + size);
            memoryLurCache = new MemoryLurCache(size / 8);
        }
    }

    /**
     *
     * @param context
     * @param maxSize - size in KB;
     */
    public static void init(Context context, int maxSize) {
        if (diskLruCache != null || !ImageUtils.isExternalStorageReady()) {
            Log.e("test", "isExternalStorageReady: " + ImageUtils.isExternalStorageReady());
            return;
        }

        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long availableBytes = statFs.getAvailableBytes() / 1024;
        int size = maxSize;
        if (availableBytes < maxSize) {
            size = (int) availableBytes / 2;
        }
        diskLruCache = DiskLruCache.getDiskLruCache(context, size);
        Log.e("test", "DiskLruCache is initialized.");
    }


    public static VdbImageListener getImageListener(final ImageView view, final int
            defaultImageResId, final int errorImageResId) {
        return new VdbImageListener() {
            @Override
            public void onResponse(VdbImageContainer response, boolean isImmediate) {
                if (response.getBitmap() != null) {
                    view.setImageBitmap(response.getBitmap());
                } else if (defaultImageResId != 0) {
                    view.setImageResource(defaultImageResId);
                }
            }

            @Override
            public void onErrorResponse(SnipeError error) {
                if (errorImageResId != 0) {
                    view.setImageResource(errorImageResId);
                }
            }
        };
    }


    public interface VdbImageListener extends VdbResponse.ErrorListener {
        void onResponse(VdbImageContainer response, boolean isImmediate);
    }

    public void displayVdbImage(ClipPos clipPos, ImageView imageView) {
        displayVdbImage(clipPos, imageView, false, true);
    }

    public void displayVdbImage(ClipPos clipPos, ImageView imageView, boolean isIgnorable) {
        displayVdbImage(clipPos, imageView, isIgnorable, true);
    }

    public void displayVdbImage(ClipPos clipPos, ImageView imageView, boolean isIgnorable, boolean useCache) {
        mUseCache = useCache;
        VdbImageListener listener = VdbImageLoader.getImageListener(imageView, 0, 0);
        get(clipPos, listener, 0, 0, ImageView.ScaleType.CENTER_INSIDE, isIgnorable);
    }

    public VdbImageContainer get(final ClipPos clipPos, final VdbImageListener
            listener, int maxWidth, int maxHeight, ScaleType scaleType, boolean isIgnorable) {
        throwIfNotOnMainThread();

        final String cacheKey = getCacheKey(clipPos, maxWidth, maxHeight, scaleType);
        //Log.e("test", "cacheKey: " + cacheKey);
        VdbImageContainer imageContainer;
        Bitmap bitmap = loadImageFromCache(cacheKey);
        if (bitmap != null && !bitmap.isRecycled()) {
            imageContainer = new VdbImageContainer(bitmap, clipPos, listener);
            listener.onResponse(imageContainer, true);
        } else {
            imageContainer = new VdbImageContainer(null, clipPos, listener);
            listener.onResponse(imageContainer, true);
            VdbRequest<Bitmap> newRequest = makeVdbImageRequest(clipPos, maxWidth, maxHeight,
                    scaleType, cacheKey);
            newRequest.setIgnorable(isIgnorable);
            mRequestQueue.add(newRequest);
            mInFlightRequest.put(cacheKey, new BatchedVdbImageRequest(newRequest, imageContainer));
        }
        return imageContainer;
    }

    private Bitmap loadImageFromCache(String cacheKey) {
        if (!mUseCache) {
            return null;
        }

        Bitmap bitmap = memoryLurCache.get(cacheKey);
        if (bitmap != null) {
            //Log.e(TAG, "Use mem cached Bitmap" + "; cacheKey: " + cacheKey);
            return bitmap;
        }
        if (diskLruCache != null) {
            bitmap = diskLruCache.get(cacheKey);
            if (bitmap != null) {
                //Log.e(TAG, "Use mem cached Bitmap" + "; cacheKey: " + cacheKey);
                return bitmap;
            }
        }
        return null;
    }

    private void cacheImages(String cacheKey, Bitmap bitmap) {
        if (mUseCache) {
            memoryLurCache.put(cacheKey, bitmap);
            if (diskLruCache != null) {
                diskLruCache.put(cacheKey, bitmap);
            }
        }
    }


    protected VdbRequest<Bitmap> makeVdbImageRequest(ClipPos clipPos, int maxWidth,
                                                     int maxHeight, ScaleType scaleType,
                                                     final String cacheKey) {

        return new VdbImageRequest(clipPos, new VdbResponse.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                onGetImageSuccess(cacheKey, response);
            }
        }, maxWidth, maxHeight, scaleType, Bitmap.Config.RGB_565, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                onGetImageError(cacheKey, error);
            }
        });
    }


    protected void onGetImageSuccess(String cacheKey, Bitmap response) {
        BatchedVdbImageRequest request = mInFlightRequest.remove(cacheKey);
        if (request != null) {
            request.mResponseBitmap = response;
            batchResponse(cacheKey, request);
        }
    }

    protected void onGetImageError(String cacheKey, SnipeError error) {
        Log.e(TAG, "onGetImageError");
    }


    public class VdbImageContainer {
        private Bitmap mBitmap;
        private final VdbImageListener mListener;
        private final ClipPos mClipPos;

        public VdbImageContainer(Bitmap bitmap, ClipPos clipPos, VdbImageListener
                listener) {
            this.mBitmap = bitmap;
            this.mClipPos = clipPos;
            this.mListener = listener;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }
    }


    private class BatchedVdbImageRequest {
        private final VdbRequest<?> mRequest;

        private Bitmap mResponseBitmap;

        private final LinkedList<VdbImageContainer> mContainers = new LinkedList<>();
        private SnipeError mError;

        public BatchedVdbImageRequest(VdbRequest<?> request, VdbImageContainer container) {
            mRequest = request;
            mContainers.add(container);
        }


        public void setError(SnipeError error) {
            mError = error;
        }


        public SnipeError getError() {
            return mError;
        }

        public void addContainer(VdbImageContainer container) {
            mContainers.add(container);
        }
    }


    private void batchResponse(final String cacheKey, BatchedVdbImageRequest request) {
        mBatchedResponses.put(cacheKey, request);

        if (mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    for (String key : mBatchedResponses.keySet()) {
                        BatchedVdbImageRequest bir = mBatchedResponses.get(key);
                        for (VdbImageContainer container : bir.mContainers) {
                            if (container.mListener == null) {
                                continue;
                            }

                            if (bir.getError() == null) {
                                container.mBitmap = bir.mResponseBitmap;
                                container.mListener.onResponse(container, false);
                                cacheImages(key, bir.mResponseBitmap);
                            }
                        }
                        mBatchedResponses.remove(key);
                    }
                    mRunnable = null;

                }

            };
            mHandler.postDelayed(mRunnable, mBatchedResponsesDelayMs);
        }
    }

    private void throwIfNotOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("VdbImageLoader must be invoked from the main thread");
        }
    }

    private static String getCacheKey(ClipPos clipPos, int maxWidth, int maxHeight,
                                      ScaleType scaleType) {
        String clipId = clipPos.vdbId == null ? String.valueOf(clipPos.cid.hashCode()) : clipPos.vdbId;
        return DigitUtils.md5(clipId
                + "#T" + clipPos.getClipTimeMs()
                + "#W" + maxWidth
                + "#H" + maxHeight
                + "#S" + scaleType.ordinal());
    }

    public static void setMemoryCache(MemoryLurCache cache) {
        if (cache == null) {
            return;
        }

        if (memoryLurCache != null) {
            memoryLurCache.evictAll();
        }
        memoryLurCache = cache;
    }

    public static void setDiskLruCache(DiskLruCache cache) {
        if (cache == null) {
            return;
        }

        if (diskLruCache != null) {
            diskLruCache.evictAll();
        }
        diskLruCache = cache;
    }
}
