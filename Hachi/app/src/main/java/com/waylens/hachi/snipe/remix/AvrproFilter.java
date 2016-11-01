package com.waylens.hachi.snipe.remix;

import com.orhanobut.logger.Logger;

import java.io.File;

/**
 * Created by laina on 16/10/27.
 */

public class AvrproFilter {
    public static String TAG = AvrproFilter.class.getSimpleName();

    public static String DEFAULT_REMIX_DIR = "smart_remix";
    public static int SMART_RANDOMCUTTING = 0;
    public static int SMART_FAST_FURIOUS = 1;
    public static int SMART_ACCELERATION = 2;
    public static int SMART_SHARPTURN = 3;
    public static int SMART_BUMPINGHARD = 4;
    public static int SMART_MAX_INDEX = 5;


    public static int DEVICE_AVF = 0;
    public static int DEVICE_IOS = 1;
    public static int DEVICE_ANDROID = 2;


    int mType;
    String mDirectory;
    int mLength;

    static {
        System.loadLibrary("avrpro-lib");
    }

    public AvrproFilter(int type, File directory, int length) {
        mType = type;
        //File dir = new File(directory.getAbsolutePath(), DEFAULT_REMIX_DIR);
        mDirectory = directory.getAbsolutePath();
        Logger.t(TAG).d("" + mDirectory);
        mLength = length;
    }

    public int init() {
        return native_avrpro_smart_filter_init(mType, mDirectory, mLength);
    }


    public native int native_avrpro_smart_filter_init(int type, String local_directory, int target_length);

    public native boolean native_avrpro_smart_filter_is_data_parsed(int filter, AvrproClipInfo clipInfo, int offset, int duration);

    public native int native_avrpro_smart_filter_feed_data(int filter, byte[] dataBuf, int size, int sourceType);

    public native AvrproSegmentInfo native_avrpro_smart_filter_read_results(int filter, boolean fromStart);

    public native int native_avrpro_smart_filter_deint(int filter);

}
