package com.waylens.hachi.snipe;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.snipe.toolbox.VdbImageRequest;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by Xiaofei on 2015/8/25.
 */
public class VdbImageLoader {
    private static final String TAG = VdbImageLoader.class.getSimpleName();
    private final VdbRequestQueue mRequestQueue;
    private final int mBatchedResponsesDelayMs = 100;

    public VdbImageLoader(VdbRequestQueue queue) {
        this.mRequestQueue = queue;
    }


    private final HashMap<String, BatchedVdbImageRequest> mInFlightRequest = new HashMap<>();

    private final HashMap<String, BatchedVdbImageRequest> mBatchedResponses = new HashMap<>();

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private Runnable mRunnable;

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
        VdbImageListener listener = VdbImageLoader.getImageListener(imageView, 0, 0);
        get(clipPos, listener);
    }


    public VdbImageContainer get(final ClipPos clipPos, final VdbImageListener
        listener) {
        return get(clipPos, listener, 0, 0);
    }

    public VdbImageContainer get(final ClipPos clipPos, final VdbImageListener
        listener, int maxWidth, int maxHeight) {
        return get(clipPos, listener, maxWidth, maxHeight, ImageView.ScaleType.CENTER_INSIDE);
    }

    public VdbImageContainer get(final ClipPos clipPos, final VdbImageListener
        listener, int maxWidth, int maxHeight, ScaleType scaleType) {
        throwIfNotOnMainThread();

        final String cacheKey = getCacheKey(clipPos, maxWidth, maxHeight, scaleType);
        Logger.t(TAG).d("cache key = " + cacheKey);

        // TODO: we need implement cache here:



        VdbImageContainer imageContainer = new VdbImageContainer(null, clipPos, listener);

        listener.onResponse(imageContainer, true);

        VdbRequest<Bitmap> newRequest = makeVdbImageRequest(clipPos, maxWidth, maxHeight,
            scaleType, cacheKey);
        mRequestQueue.add(newRequest);
        mInFlightRequest.put(cacheKey, new BatchedVdbImageRequest(newRequest, imageContainer));
        return imageContainer;
    }


    protected VdbRequest<Bitmap> makeVdbImageRequest(ClipPos clipPos, int maxWidth,
                                                     int maxHeight, ScaleType scaleType, final
                                                     String cacheKey) {

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


    private void batchResponse(String cacheKey, BatchedVdbImageRequest request) {
        mBatchedResponses.put(cacheKey, request);

        if (mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    for (BatchedVdbImageRequest bir : mBatchedResponses.values()) {
                        for (VdbImageContainer container : bir.mContainers) {
                            if (container.mListener == null) {
                                continue;
                            }

                            if (bir.getError() == null) {
                                container.mBitmap = bir.mResponseBitmap;
                                container.mListener.onResponse(container, false);
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
        return "#CID" + clipPos.cid.type + "#" + clipPos.cid.subType + "#" + clipPos
            .getClipTimeMs() + "#W" + maxWidth
            + "#H" + maxHeight + "#S" + scaleType.ordinal();
    }
}
