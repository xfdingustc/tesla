package com.transee.vdb;

import android.os.Handler;


abstract public class Vdb {

    public static final int VERSION_1_0 = 0; // initial version

    // implemented by CameraVideoActivity
    public interface Callback {


    }

    protected int mVersion = VERSION_1_0;
    protected final Handler mHandler;
    protected final Callback mCallback;

    // will run on caller's thread
    public Vdb(Callback callback) {
        mHandler = new Handler();
        mCallback = callback;
    }

    // API
    public int getVersion() {
        return mVersion;
    }

    // API
    public abstract boolean isLocal();

    // API
    public abstract void start(String hostString);

    // API
    public abstract void stop();


    // API
    public abstract VdbClient getClient();


}
