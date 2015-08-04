package com.waylens.hachi.app;

import android.app.Application;

import com.orhanobut.logger.Logger;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class Hachi extends Application {
    private static final String TAG = Hachi.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        initLogger();
    }

    private void initLogger() {
        Logger
            .init(TAG)
            .setMethodCount(1)
            .hideThreadInfo();
    }
}
