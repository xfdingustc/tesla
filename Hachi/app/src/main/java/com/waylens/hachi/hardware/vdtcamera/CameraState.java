package com.waylens.hachi.hardware.vdtcamera;

public class CameraState {
    public static final String TAG = CameraState.class.getSimpleName();


    public static final int VIDEO_RESOLUTION_UNKNOWN = -1;
    public static final int VIDEO_RESOLUTION_1080P30 = 0;
    public static final int VIDEO_RESOLUTION_1080P60 = 1;
    public static final int VIDEO_RESOLUTION_720P30 = 2;
    public static final int VIDEO_RESOLUTION_720P60 = 3;
    public static final int VIDEO_RESOLUTION_4KP30 = 4;
    public static final int VIDEO_RESOLUTION_4KP60 = 5;
    public static final int VIDEO_RESOLUTION_480P30 = 6;
    public static final int VIDEO_RESOLUTION_480P60 = 7;
    public static final int VIDEO_RESOLUTION_720P120 = 8;
    public static final int VIDEO_RESOLUTION_STILL = 9;
    public static final int VIDEO_RESOLUTION_NUM = 10;

    public static final int VIDEO_RESOLUTION_720P = 0;
    public static final int VIDEO_RESOLUTION_1080P = 1;

    public static final int VIDEO_FRAMERATE_30FPS = 0;
    public static final int VIDEO_FRAMERATE_60FPS = 1;
    public static final int VIDEO_FRAMERATE_120FPS = 2;

    public static final int VIDEO_QUALITY_UNKNOWN = -1;
    public static final int VIDEO_QUALITY_SUPPER = 0;
    public static final int VIDEO_QUALITY_HI = 1;
    public static final int VIDEO_QUALITY_MID = 2;
    public static final int VIDEO_QUALITY_LOW = 3;
    public static final int VIDEO_QUALITY_NUM = 4;

    public static final int REC_MODE_UNKNOWN = -1;
    public static final int FLAG_AUTO_RECORD = 1 << 0;
    public static final int FLAG_LOOP_RECORD = 1 << 1;
    public static final int REC_MODE_MANUAL = 0;
    public static final int REC_MODE_AUTOSTART = FLAG_AUTO_RECORD;
    public static final int REC_MODE_MANUAL_LOOP = FLAG_LOOP_RECORD;
    public static final int REC_MODE_AUTOSTART_LOOP = (FLAG_AUTO_RECORD | FLAG_LOOP_RECORD);

    public static final int COLOR_MODE_UNKNOWN = -1;
    public static final int COLOR_MODE_NORMAL = 0;
    public static final int COLOR_MODE_SPORT = 1;
    public static final int COLOR_MODE_CARDV = 2;
    public static final int COLOR_MODE_SCENE = 3;
    public static final int COLOR_MODE_NUM = 4;

    public static final int ERROR_START_RECORD_OK = 0;
    public static final int ERROR_START_RECORD_NO_CARD = 1;
    public static final int ERROR_START_RECORD_CARD_FULL = 2;
    public static final int ERROR_START_RECORD_CARD_ERROR = 3;

    public interface OnStateChangeListener {
        void onStateChange();
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.mListener = listener;
    }

    private OnStateChangeListener mListener = null;


    private boolean mbIsStill = false;
    private int mRecordDuration = -1;
    private boolean mbRecordDurationUpdated; //
    private long mRecordTimeFetchedTime;


    private int mOverlayFlags = -1;

    private int mVideoResolutionList = 0;
    private int mVideoResolutionIndex = VIDEO_RESOLUTION_UNKNOWN;

    private int mVideoQualityList = 0;
    private int mVideoQualityIndex = VIDEO_QUALITY_UNKNOWN;

    private int mRecordModeList = 0;
    private int mRecordModeIndex = REC_MODE_UNKNOWN;

    private int mColorModeList = 0;
    private int mColorModeIndex = COLOR_MODE_UNKNOWN;

    private int mMarkBeforeTime = -1;
    private int mMarkAfterTime = -1;


    private void notifyStateChanged() {
        if (mListener != null) {
            mListener.onStateChange();
        }
    }

    public boolean canDoStillCapture() {
        return (mVideoResolutionList & (1 << VIDEO_RESOLUTION_STILL)) != 0;
    }


    public void setOverlayFlags(int flags) {
        if (mOverlayFlags != flags) {
//            Logger.t(TAG).d("setOverlayFlags: " + Integer.toHexString(flags));
            mOverlayFlags = flags;
            notifyStateChanged();
        }
    }

    public void setVideoResolutionList(int list) {
        if (mVideoResolutionList != list) {

//            Logger.t(TAG).d("setVideoResolutionList: " + Integer.toHexString(list));
            mVideoResolutionList = list;
            notifyStateChanged();
        }
    }

    public void setVideoResolution(int index) {
        if (mVideoResolutionIndex != index) {
//            Logger.t(TAG).d("setVideoResolution: " + index);
            mVideoResolutionIndex = index;
            notifyStateChanged();
        }
    }

    public int getVideoResolution() {
        switch (mVideoQualityIndex) {
            case VIDEO_RESOLUTION_1080P30:
            case VIDEO_RESOLUTION_1080P60:

                return VIDEO_RESOLUTION_1080P;
            default:
                return VIDEO_RESOLUTION_720P;
        }
    }

    public int getVideoFramerate() {
        switch (mVideoQualityIndex) {
            case VIDEO_RESOLUTION_1080P30:
            case VIDEO_RESOLUTION_4KP30:
            case VIDEO_RESOLUTION_480P30:
            case VIDEO_RESOLUTION_720P30:
                return VIDEO_FRAMERATE_30FPS;

            case VIDEO_RESOLUTION_1080P60:
            case VIDEO_RESOLUTION_720P60:
            case VIDEO_RESOLUTION_4KP60:
            case VIDEO_RESOLUTION_480P60:
                return VIDEO_FRAMERATE_60FPS;


            case VIDEO_RESOLUTION_720P120:
                return VIDEO_FRAMERATE_120FPS;
            default:
                return VIDEO_FRAMERATE_30FPS;

        }
    }

    public void setVideoQualityList(int list) {
        if (mVideoQualityList != list) {

//            Logger.t(TAG).d("setVideoQualityList: " + Integer.toHexString(list));

            mVideoQualityList = list;
            notifyStateChanged();
        }
    }

    public void setVideoQuality(int index) {
        if (mVideoQualityIndex != index) {

//            Logger.t(TAG).d("setVideoQuality: " + index);

            mVideoQualityIndex = index;
            notifyStateChanged();
        }
    }

    public void setRecordModeList(int list) {
        if (mRecordModeList != list) {

//            Logger.t(TAG).d("setRecordModeList: " + Integer.toHexString(list));
            mRecordModeList = list;
            notifyStateChanged();
        }
    }

    public void setRecordMode(int index) {
        if (mRecordModeIndex != index) {

//            Logger.t(TAG).d("setRecordMode: " + index);
            mRecordModeIndex = index;
            notifyStateChanged();
        }
    }

    public void setColorModeList(int list) {
        if (mColorModeList != list) {

//            Logger.t(TAG).d("setColorModeList: " + Integer.toHexString(list));
            mColorModeList = list;
            notifyStateChanged();
        }
    }

    public void setColorMode(int index) {
        if (mColorModeIndex != index) {

//            Logger.t(TAG).d("setColorMode: " + index);
            mColorModeIndex = index;
            notifyStateChanged();
        }
    }

    public void setMarkTime(int before, int after) {
        if (mMarkBeforeTime != before || mMarkAfterTime != after) {
            mMarkBeforeTime = before;
            mMarkAfterTime = after;
            notifyStateChanged();
        }
    }

    //public int getRecordState() {
//        return mRecordState;
//    }

    public int getRecordMode() {
        return mRecordModeIndex;
    }


}
