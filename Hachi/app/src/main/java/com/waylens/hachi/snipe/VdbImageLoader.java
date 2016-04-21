package com.waylens.hachi.snipe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.cache.DiskCache;
import com.waylens.hachi.snipe.cache.impl.UnlimitedDiskCache;
import com.waylens.hachi.snipe.cache.impl.ext.LruDiskCache;
import com.waylens.hachi.snipe.cache.naming.FileNameGenerator;
import com.waylens.hachi.snipe.cache.naming.HashCodeFileNameGenerator;
import com.waylens.hachi.snipe.toolbox.VdbImageRequest;
import com.waylens.hachi.snipe.utils.StorageUtils;
import com.waylens.hachi.utils.DigitUtils;
import com.waylens.hachi.vdb.ClipPos;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Xiaofei on 2015/8/25.
 */
public class VdbImageLoader {
    private static final String TAG = VdbImageLoader.class.getSimpleName();
    private VdbRequestQueue mRequestQueue;
    private final int mBatchedResponsesDelayMs = 100;

    final HashMap<String, BatchedVdbImageRequest> mInFlightRequest = new HashMap<>();

    final ConcurrentHashMap<String, BatchedVdbImageRequest> mBatchedResponses = new ConcurrentHashMap<>();

    final Handler mHandler = new Handler(Looper.getMainLooper());
    final Handler mBackgroundHandler;
    Runnable mRunnable;


    private static DiskCache mCache;



    public static VdbImageLoader getImageLoader(VdbRequestQueue queue) {
        VdbImageLoader imageLoader = new VdbImageLoader();
        if (queue != null) {
            imageLoader.mRequestQueue = queue;
        }
        return imageLoader;
    }


    protected VdbImageLoader() {
        HandlerThread backgroundThread = new HandlerThread("VdbImageLoader");
        backgroundThread.start();
        mBackgroundHandler = new Handler(backgroundThread.getLooper());

    }

    /**
     * init
     *
     * @param context - context
     * @param maxSize - size in KB;
     */
    public static void init(Context context, int maxSize) {


        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long availableBytes = statFs.getAvailableBytes() / 1024;
        int size = maxSize;
        if (availableBytes < maxSize) {
            size = (int) availableBytes / 2;
        }
        mCache = createDiskCache(context, new HashCodeFileNameGenerator(), 1024 * 1024 * 1024, 50 * 1024);
        Log.e("test", "DiskLruCache is initialized.");
    }


    public static DiskCache createDiskCache(Context context, FileNameGenerator diskCacheFileNameGenerator,
                                            long diskCacheSize, int diskCacheFileCount) {
        File reserveCacheDir = createReserveDiskCacheDir(context);
        if (diskCacheSize > 0 || diskCacheFileCount > 0) {
            File individualCacheDir = StorageUtils.getIndividualCacheDirectory(context);
            try {
                return new LruDiskCache(individualCacheDir, reserveCacheDir, diskCacheFileNameGenerator, diskCacheSize,
                    diskCacheFileCount);
            } catch (IOException e) {
                Logger.e("" + e);
                // continue and create unlimited cache
            }
        }
        File cacheDir = StorageUtils.getCacheDirectory(context);
        return new UnlimitedDiskCache(cacheDir, reserveCacheDir, diskCacheFileNameGenerator);
    }

    private static File createReserveDiskCacheDir(Context context) {
        File cacheDir = StorageUtils.getCacheDirectory(context, false);
        File individualDir = new File(cacheDir, "uil-images");
        if (individualDir.exists() || individualDir.mkdir()) {
            cacheDir = individualDir;
        }
        return cacheDir;
    }

    public static VdbImageListener getImageListener(final ImageView view, final int
        defaultImageResId, final int errorImageResId) {
        return new VdbImageListener() {
            @Override
            public void onResponse(VdbImageContainer imageContainer, boolean isImmediate) {
                if (view == null) {
                    return;
                }
                if (imageContainer.getBitmap() != null) {
                    view.setImageBitmap(imageContainer.getBitmap());

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

    public void displayVdbImage(ClipPos clipPos, ImageView imageView, int maxWidth, int maxHeight) {
        //mUseCache = true;
        VdbImageListener listener = VdbImageLoader.getImageListener(imageView, 0, 0);
        get(clipPos, listener, maxWidth, maxHeight, ImageView.ScaleType.CENTER_INSIDE, false);
    }

    public void displayVdbImage(ClipPos clipPos, ImageView imageView, boolean isIgnorable, boolean useCache) {
        //mUseCache = useCache;
        VdbImageListener listener = VdbImageLoader.getImageListener(imageView, 0, 0);
        get(clipPos, listener, imageView.getWidth(), imageView.getHeight(), ImageView.ScaleType.CENTER_INSIDE, isIgnorable);
    }

    VdbImageContainer get(final ClipPos clipPos, final VdbImageListener listener, final int maxWidth,
                          final int maxHeight, final ScaleType scaleType, final boolean isIgnorable) {

        throwIfNotOnMainThread();

//        mBackgroundHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                final String cacheKey = getCacheKey(clipPos, maxWidth, maxHeight, scaleType);
//                Bitmap bitmap = loadImageFromCache(cacheKey);
//                if (bitmap != null && !bitmap.isRecycled()) {
//                    VdbImageContainer imageContainer = new VdbImageContainer(bitmap, clipPos, listener);
//                    showImage(imageContainer);
//                } else {
//                    VdbImageContainer imageContainer = new VdbImageContainer(null, clipPos, listener);
//                    showImage(imageContainer);
//                    VdbImageRequest newRequest = makeVdbImageRequest(clipPos, maxWidth, maxHeight,
//                        scaleType, cacheKey);
//                    newRequest.setIgnorable(isIgnorable);
//                    mRequestQueue.add(newRequest);
//                    mInFlightRequest.put(cacheKey, new BatchedVdbImageRequest(newRequest, imageContainer));
//                }
//            }
//        });
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                String cacheKey = getCacheKey(clipPos, maxWidth, maxHeight, scaleType);
                Bitmap cachedBitmap = tryLoadBitmap(cacheKey);
                if (cachedBitmap != null) {
//                    Logger.t(TAG).d("hit cache");
                    VdbImageContainer container = new VdbImageContainer(cachedBitmap, clipPos, null, listener);
//                    listener.onResponse(container, true);
                    showImage(container);
                    return;
                }

                VdbImageContainer imageContainer = new VdbImageContainer(null, clipPos, cacheKey, listener);

                listener.onResponse(imageContainer, true);

                BatchedVdbImageRequest request = mInFlightRequest.get(cacheKey);
                if (request != null) {
                    request.addContainer(imageContainer);
                    showImage(imageContainer);
                    return;
                }

                VdbImageRequest newRequest = makeVdbImageRequest(clipPos, maxWidth, maxHeight, scaleType, cacheKey);
                newRequest.setIgnorable(isIgnorable);
                mRequestQueue.add(newRequest);

                mInFlightRequest.put(cacheKey, new BatchedVdbImageRequest(newRequest, imageContainer));
                showImage(imageContainer);
                return;
            }
        });

        return null;

    }

    public Bitmap tryLoadBitmap(String cacheKey) {
        Bitmap bitmap = null;
        File imageFile = mCache.get(cacheKey);
        if (imageFile != null && imageFile.exists() && imageFile.length() > 0) {
//            L.d(LOG_LOAD_IMAGE_FROM_DISK_CACHE, memoryCacheKey);
//            loadedFrom = LoadedFrom.DISC_CACHE;
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), decodeOptions);

        }

        return bitmap;

    }

    void showImage(final VdbImageContainer imageContainer) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                imageContainer.mListener.onResponse(imageContainer, true);
            }
        });
    }


    protected VdbImageRequest makeVdbImageRequest(ClipPos clipPos, int maxWidth,
                                                  int maxHeight, ScaleType scaleType,
                                                  final String cacheKey) {

        return new VdbImageRequest(clipPos,
            new VdbResponse.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap response) {
                    onGetImageSuccess(cacheKey, response);
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    onGetImageError(cacheKey, error);
                }
            },
            maxWidth, maxHeight, scaleType, Bitmap.Config.RGB_565, cacheKey);
    }


    protected void onGetImageSuccess(String cacheKey, Bitmap response) {

        BatchedVdbImageRequest request = mInFlightRequest.remove(cacheKey);
        if (request != null) {
            request.mResponseBitmap = response;
            batchResponse(cacheKey, request);
        }
    }

    protected void onGetImageError(String cacheKey, SnipeError error) {
        Log.e(TAG, "onGetImageError: " + error);
    }


    public class VdbImageContainer {
        private Bitmap mBitmap;
        private final VdbImageListener mListener;
        private final ClipPos mClipPos;
        private final String mCacheKey;

        public VdbImageContainer(Bitmap bitmap, ClipPos clipPos, String cacheKey, VdbImageListener listener) {
            this.mBitmap = bitmap;
            this.mClipPos = clipPos;
            this.mCacheKey = cacheKey;
            this.mListener = listener;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }
    }


    class BatchedVdbImageRequest {
        private final VdbImageRequest mRequest;

        private Bitmap mResponseBitmap;

        private final LinkedList<VdbImageContainer> mContainers = new LinkedList<>();
        private SnipeError mError;

        public BatchedVdbImageRequest(VdbImageRequest request, VdbImageContainer container) {
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
                    for (final String key : mBatchedResponses.keySet()) {
                        final BatchedVdbImageRequest bir = mBatchedResponses.get(key);
                        for (VdbImageContainer container : bir.mContainers) {
                            if (container.mListener == null) {
                                continue;
                            }

                            if (bir.getError() == null) {
                                container.mBitmap = bir.mResponseBitmap;
                                container.mListener.onResponse(container, false);
                                mBackgroundHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            mCache.save(cacheKey, bir.mResponseBitmap);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                            }
                        }

                    }
                    mBatchedResponses.clear();
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
        //Log.e("test", String.format("====== clipId[%s],clipTime[%d], w[%d], h[%d], scale[%d]",
        //        clipId, clipPos.getClipTimeMs(), maxWidth, maxHeight, scaleType.ordinal()));
//        return DigitUtils.md5(clipId
//                + "#T" + clipPos.getClipTimeMs()
//                + "#W" + maxWidth
//                + "#H" + maxHeight
//                + "#S" + scaleType.ordinal());
        return DigitUtils.md5(clipId
            + "#T" + clipPos.getClipTimeMs());
    }
}
