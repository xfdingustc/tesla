package com.waylens.hachi.snipe.remix;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.util.List;

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
    public static int SMART_SMOOTH_RUNNING = 5;
    public static int SMART_RANDOMPICK = 6;
    public static int SMART_MAX_INDEX = 7;

/*
    SMART_RANDOMCUTTING = 0,    // a mixture of all types
    SMART_FAST_FURIOUS,     // speed in a high range
    SMART_ACCELERATION,     // dramatic gforce change
    SMART_SHARPTURN,        // a sharp turn
    SMART_BUMPINGHARD,      // driving on a bumping road
    SMART_SMOOTH_RUNNING,   // driving at a constant high speed
    SMART_RANDOMPICK,       // random pick segments from highlights
    SMART_MAX_INDEX
*/

    public static int DEVICE_AVF = 0;
    public static int DEVICE_IOS = 1;
    public static int DEVICE_ANDROID = 2;
    public static final int MODE_SMART_REMIX = 0x01;
    public static final int MODE_LAP_TIMER = 0x02;

    private int mWorkMode;
    int mType;
    String mDirectory;
    int mLength;
    private AvrproClipInfo mClipInfo;

    static {
        System.loadLibrary("avrpro-lib");
    }

    public AvrproFilter(int type, File directory, int length) {
        mType = type;
        //File dir = new File(directory.getAbsolutePath(), DEFAULT_REMIX_DIR);
        mDirectory = directory.getAbsolutePath();
        Logger.t(TAG).d("" + mDirectory);
        mLength = length;
        mWorkMode = MODE_SMART_REMIX;
    }

    public AvrproFilter(AvrproClipInfo clipInfo) {
        mWorkMode = MODE_LAP_TIMER;
        mClipInfo = clipInfo;
    }

    public AvrproClipInfo getClipInfo() {
        return mClipInfo;
    }

    public int init() {
        switch (mWorkMode) {
            case MODE_SMART_REMIX:
                return native_avrpro_smart_filter_init(mType, mDirectory, mLength);
            case MODE_LAP_TIMER:
                return native_avrpro_lap_timer_filter_init(mClipInfo);
            default:
                return -1;
        }
    }

    public native int native_avrpro_smart_filter_init(int type, String local_directory, int target_length);

    public native boolean native_avrpro_smart_filter_is_data_parsed(int filter, AvrproClipInfo clipInfo, int offset, int duration);

    public native int native_avrpro_smart_filter_feed_data(int filter, byte[] dataBuf, int size, int sourceType);

    public native AvrproSegmentInfo native_avrpro_smart_filter_read_results(int filter, boolean fromStart);

    public native int native_avrpro_smart_filter_deint(int filter);

    public native int native_avrpro_lap_timer_filter_init(AvrproClipInfo clipInfo);

    public native int native_avrpro_lap_timer_set_start(int filter, AvrproGpsParsedData startNode);

    public native int native_avrpro_lap_timer_feed_gps_data(int filter, byte[] dataBuf, int size, int sourceType);

    public native AvrproLapTimerResult native_avrpro_lap_timer_read_results(int filter);

    public native int native_avrpro_lap_timer_deinit(int filter);

}
