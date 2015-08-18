package com.waylens.hachi.VdbImageLoader.core;

import com.orhanobut.logger.Logger;
import com.transee.vdb.Clip;
import com.transee.vdb.ClipPos;
import com.transee.vdb.Vdb;
import com.waylens.hachi.VdbImageLoader.LoadAndDisplayVdbImageTask;
import com.waylens.hachi.VdbImageLoader.core.imageaware.VdbImageAware;
import com.waylens.hachi.VdbImageLoader.VdbImageLoadingInfo;

/**
 * Created by Xiaofei on 2015/8/14.
 */
public class VdbImageLoader {
    public final static String TAG = VdbImageLoader.class.getSimpleName();

    private static final String ERROR_INIT_CONFIG_WITH_NULL = "VdbImageLoader configuration " +
        "cannot be initialized with null";

    private VdbImageLoaderConfiguration mConfiguration;
    private VdbImageLoaderEngine mEngine;

    private volatile static VdbImageLoader mSharedInstance;

    public static VdbImageLoader getInstance() {
        if (mSharedInstance == null) {
            synchronized (VdbImageLoader.class) {
                if (mSharedInstance == null) {
                    mSharedInstance = new VdbImageLoader();
                }
            }
        }
        return mSharedInstance;
    }

    protected VdbImageLoader() {

    }

    public synchronized void init(VdbImageLoaderConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException(ERROR_INIT_CONFIG_WITH_NULL);
        }

        if (this.mConfiguration == null) {
            Logger.t(TAG).d("Initialize VdbImageLoader with configuration");
            mEngine = new VdbImageLoaderEngine(configuration);
            this.mConfiguration = configuration;
        } else {
            Logger.t(TAG).w("VdbImageLoader is already inited");
        }
    }

    public void displayImage(Vdb vdb, Clip clip, ClipPos clipPos, VdbImageAware imageAware) {
        VdbImageLoadingInfo imageLoadingInfo = new VdbImageLoadingInfo(vdb, clip, clipPos,
            imageAware);
        LoadAndDisplayVdbImageTask displayTask = new LoadAndDisplayVdbImageTask(mEngine,
            imageLoadingInfo);

        mEngine.submit(displayTask);

    }

}
