package com.waylens.hachi.glide_snipe_integration;

import android.content.Context;


import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.library.vdb.ClipPos;
import com.waylens.hachi.library.snipe.VdbRequestQueue;


import java.io.InputStream;

/**
 * Created by Xiaofei on 2016/6/18.
 */
public class SnipeGlideLoader implements StreamModelLoader<ClipPos> {

    public static class Factory implements ModelLoaderFactory<ClipPos, InputStream> {
        private static VdbRequestQueue internalQueue;
        private VdbRequestQueue requestQueue;
        private final SnipeRequestFactory requestFactory;

        private static VdbRequestQueue getInternalQueue(Context context) {
            if (internalQueue == null) {
                synchronized (Factory.class) {
                    if (internalQueue == null) {
                        VdtCameraManager manager = VdtCameraManager.getManager();
                        if (manager.isConnected()) {
                            VdtCamera vdtCamera = VdtCameraManager.getManager().getCurrentCamera();
                            if (vdtCamera == null) {
                                internalQueue = null;
                            } else {
                                internalQueue = vdtCamera.getRequestQueue();
                            }
                        }
                    }
                }
            }
            return internalQueue;
        }

        public Factory(Context context) {
            this(getInternalQueue(context));
        }

        public Factory(VdbRequestQueue requestQueue) {
            this(requestQueue, SnipeStreamFetcher.DEFAULT_REQUEST_FACTORY);
        }

        public Factory(VdbRequestQueue requestQueue, SnipeRequestFactory requestFactory) {
            this.requestFactory = requestFactory;
            this.requestQueue = requestQueue;
        }

        @Override
        public ModelLoader<ClipPos, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new SnipeGlideLoader(requestQueue, requestFactory);
        }

        @Override
        public void teardown() {

        }
    }

    private final VdbRequestQueue mVdbRequestQueue;
    private final SnipeRequestFactory mVdbRequestFactory;

    public SnipeGlideLoader(VdbRequestQueue requestQueue) {
        this(requestQueue, SnipeStreamFetcher.DEFAULT_REQUEST_FACTORY);

    }


    public SnipeGlideLoader(VdbRequestQueue requestQueue, SnipeRequestFactory requestFactory) {
        this.mVdbRequestQueue = requestQueue;
        this.mVdbRequestFactory = requestFactory;
    }


    @Override
    public DataFetcher<InputStream> getResourceFetcher(ClipPos model, int width, int height) {
        return new SnipeStreamFetcher(mVdbRequestQueue, model);
    }
}
