package com.waylens.hachi.app;

import com.github.moduth.blockcanary.BlockCanaryContext;
import com.github.moduth.blockcanary.BuildConfig;

/**
 * Created by Xiaofei on 2016/8/3.
 */
public class AppBlockCanaryContext extends BlockCanaryContext {

    @Override
    public int getConfigBlockThreshold() {
        return 500;
    }


    @Override
    public boolean isNeedDisplay() {
        return BuildConfig.DEBUG;
    }
}
