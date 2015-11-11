package com.transee.vdb;

import com.orhanobut.logger.Logger;

public class LibLoader {
    private static final String TAG = LibLoader.class.getSimpleName();
    static Object mLock = new Object();
    static boolean mbLoaded = false;

    static void load() {
        synchronized (mLock) {
            if (!mbLoaded) {
                mbLoaded = true;
                Logger.t(TAG).d("LLLLLLLLLLLLLLLLLLLLLLLLLLLL");
                System.loadLibrary("avfmedia");
            }
        }
    }

}
