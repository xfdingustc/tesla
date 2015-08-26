package com.waylens.hachi.snipe;

import android.graphics.Bitmap;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.orhanobut.logger.Logger;
import com.transee.vdb.Clip;
import com.transee.vdb.ClipPos;
import com.waylens.hachi.snipe.toolbox.VdbImageRequest;

/**
 * Created by Xiaofei on 2015/8/25.
 */
public class VdbImageLoader {
    private static final String TAG = VdbImageLoader.class.getSimpleName();
    private final VdbRequestQueue mRequestQueue;

    public VdbImageLoader(VdbRequestQueue queue) {
        this.mRequestQueue = queue;
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


    public VdbImageContainer get(final Clip clip, final ClipPos clipPos, final VdbImageListener
        listener) {
        return get(clip, clipPos, listener, 0, 0);
    }

    public VdbImageContainer get(final Clip clip, final ClipPos clipPos, final VdbImageListener
        listener, int maxWidth, int maxHeight) {
        return get(clip, clipPos, listener, maxWidth, maxHeight, ImageView.ScaleType.CENTER_INSIDE);
    }

    public VdbImageContainer get(final Clip clip, final ClipPos clipPos, final VdbImageListener
        listener, int maxWidth, int maxHeight, ScaleType scaleType) {
        throwIfNotOnMainThread();


        VdbImageContainer imageContainer = new VdbImageContainer(null, clip, clipPos, listener);

        listener.onResponse(imageContainer, true);

        VdbRequest<Bitmap> newRequest = makeVdbImageRequest(clip, clipPos, maxWidth, maxHeight,
            scaleType);
        mRequestQueue.add(newRequest);
        return imageContainer;
    }


    protected VdbRequest<Bitmap> makeVdbImageRequest(Clip clip, ClipPos clipPos, int maxWidth,
                                                     int maxHeight, ScaleType scaleType) {
        return new VdbImageRequest(clip, clipPos, new VdbResponse.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                Logger.t(TAG).d("Bitmap decoder success");
                onGetImageSuccess(response);
            }
        }, maxWidth, maxHeight, scaleType, Bitmap.Config.RGB_565, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
    }


    protected void onGetImageSuccess(Bitmap response) {

    }



    public class VdbImageContainer {
        private final Bitmap mBitmap;
        private final VdbImageListener mListener;
        private final Clip mClip;
        private final ClipPos mClipPos;

        public VdbImageContainer(Bitmap bitmap, Clip clip, ClipPos clipPos, VdbImageListener
                                 listener) {
            this.mBitmap = bitmap;
            this.mClip = clip;
            this.mClipPos = clipPos;
            this.mListener = listener;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }
    }

    private void throwIfNotOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("VdbImageLoader must be invoked from the main thread");
        }
    }
}
