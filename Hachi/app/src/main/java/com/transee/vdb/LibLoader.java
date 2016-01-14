package com.transee.vdb;

public class LibLoader {
    private static final String TAG = LibLoader.class.getSimpleName();
    static Object mLock = new Object();
    static boolean mbLoaded = false;

    public static void load() {
        synchronized (mLock) {
            if (!mbLoaded) {
                mbLoaded = true;
                System.loadLibrary("avfmedia");
            }
        }
    }

}
