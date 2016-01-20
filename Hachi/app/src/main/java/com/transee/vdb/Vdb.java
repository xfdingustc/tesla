package com.transee.vdb;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import com.transee.common.ByteStream;
import com.transee.common.GPSPath;
import com.waylens.hachi.vdb.ClipActionInfo;
import com.transee.vdb.RemoteVdbClient.BufferSpaceLowInfo;
import com.transee.vdb.VdbClient.PlaylistPlaybackUrl;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipDownloadInfo;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;

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
