package com.waylens.hachi.hardware.vdtcamera;

import android.os.SystemClock;

import com.orhanobut.logger.Logger;

import java.util.Locale;

public class CameraState {
    public static final String TAG = CameraState.class.getSimpleName();

    public static final int STATE_RECORD_UNKNOWN = -1;
    public static final int STATE_RECORD_STOPPED = 0;
    public static final int STATE_RECORD_STOPPING = 1;
    public static final int STATE_RECORD_STARTING = 2;
    public static final int STATE_RECORD_RECORDING = 3;
    public static final int STATE_RECORD_SWITCHING = 4;

    public static final int STATE_MIC_UNKNOWN = -1;
    public static final int STATE_MIC_ON = 0;
    public static final int STATE_MIC_OFF = 1;

    public static final int STATE_BATTERY_UNKNOWN = -1;
    public static final int STATE_BATTERY_FULL = 0;
    public static final int STATE_BATTERY_NOT_CHARGING = 1;
    public static final int STATE_BATTERY_DISCHARGING = 2;
    public static final int STATE_BATTERY_CHARGING = 3;

    public static final int STATE_POWER_UNKNOWN = -1;
    public static final int STATE_POWER_NO = 0;
    public static final int STATE_POWER_YES = 1;

    public static final int STATE_STORAGE_UNKNOWN = -1;
    public static final int STATE_STORAGE_NO_STORAGE = 0;
    public static final int STATE_STORAGE_LOADING = 1;
    public static final int STATE_STORAGE_READY = 2;
    public static final int STATE_STORAGE_ERROR = 3;
    public static final int STATE_STORAGE_USBDISC = 4;

    public static final int OVERLAY_FLAG_NAME = 0x01;
    public static final int OVERLAY_FLAG_TIME = 0x02;
    public static final int OVERLAY_FLAG_GPS = 0x04;
    public static final int OVERLAY_FLAG_SPEED = 0x08;

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


    private String mCameraName = new String();
    private String mFirmwareVersion = new String();
    private int mApiVersion = 0;
    private String mBuild = new String();

    private int mRecordState = STATE_RECORD_UNKNOWN;

    private boolean mbIsStill = false;
    private int mRecordDuration = -1;
    private boolean mbRecordDurationUpdated; //
    private long mRecordTimeFetchedTime;

    private int mMicState = STATE_MIC_UNKNOWN;
    private int mMicVol = -1;

    private int mBatteryState = STATE_BATTERY_UNKNOWN;
    private int mPowerState = STATE_POWER_UNKNOWN;
    private int mBatteryVol = -1;

    private int mStorageState = STATE_STORAGE_UNKNOWN;
    private long mStorageTotalSpace = 0;
    private long mStorageFreeSpace = 0;

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

    public boolean version12() {
        return mApiVersion >= makeVersion(1, 2);
    }

    private int makeVersion(int main, int sub) {
        return (main << 16) | sub;
    }

    public String versionString() {
        int main = (mApiVersion >> 16) & 0xff;
        int sub = mApiVersion & 0xffff;
        return String.format(Locale.US, "%d.%d.%s", main, sub, mBuild);
    }

    synchronized public void setCameraName(String name) {
        if (name.equals("No Named")) {
            // use empty string for unnamed camera
            name = "";
        }
        if (!mCameraName.equals(name)) {
            Logger.t(TAG).d("setCameraName: " + name);
            mCameraName = name;
            notifyStateChanged();
        }
    }

    public String getName() {
        return mCameraName;
    }

    public int getBatteryState() {
        return mBatteryState;
    }

    public int getBatteryVolume() {
        return mBatteryVol;
    }

    public int getStorageState() {
        return mStorageState;
    }

    public long getStorageTotalSpace() {
        return mStorageTotalSpace;
    }

    public long getStorageFreeSpace() {
        return mStorageFreeSpace;
    }

    public void setFirmwareVersion(String version) {
        if (!mFirmwareVersion.equals(version)) {
            Logger.t(TAG).d("setFirmwareVersion: " + version);

            mFirmwareVersion = version;
            notifyStateChanged();
        }
    }

    public void setApiVersion(int main, int sub, String build) {
        int version = makeVersion(main, sub);
        if (mApiVersion != version || !mBuild.equals(build)) {
            Logger.t(TAG).d("setApiVersion: " + version);
            mApiVersion = version;
            mBuild = build;
            notifyStateChanged();
        }
    }

    public void setRecordState(int state, boolean is_still) {
        if (mRecordState != state || mbIsStill != is_still) {
            Logger.t(TAG).d("setRecordState: " + state + ", is_still: " + is_still);
            mRecordState = state;
            mbIsStill = is_still;
            notifyStateChanged();
        }
    }

    public void setRecordDuration(int duration) {
//        Logger.t(TAG).d("setRecordDuration: " + duration);
        mRecordDuration = duration;
        if (mRecordState == STATE_RECORD_RECORDING || mRecordState == STATE_RECORD_STOPPING) {
            mRecordTimeFetchedTime = SystemClock.uptimeMillis();
            mbRecordDurationUpdated = true;
        }
        notifyStateChanged();
    }

    public void setMicState(int state, int vol) {
        if (mMicState != state || mMicVol != vol) {
//            Logger.t(TAG).d("setMicState: " + state + ", " + vol);
            mMicState = state;
            mMicVol = vol;
            notifyStateChanged();
        }
    }

    public void setPowerState(int batteryState, int powerState) {
        if (mBatteryState != batteryState || mPowerState != powerState) {
//            Logger.t(TAG).d("setPowerState: " + batteryState + "," + powerState);
            mBatteryState = batteryState;
            mPowerState = powerState;
            notifyStateChanged();
        }
    }

    public void setBatteryVol(int vol) {
        if (mBatteryVol != vol) {
//            Logger.t(TAG).d("setBatteryVol: " + vol);
            mBatteryVol = vol;
            notifyStateChanged();
        }
    }

    public void setStorageState(int state) {
        if (mStorageState != state) {
//            Logger.t(TAG).d("setStorageState: " + state);
            mStorageState = state;
            notifyStateChanged();
        }
    }

    public void setStorageSpace(long totalSpace, long freeSpace) {
        if (mStorageTotalSpace != totalSpace || mStorageFreeSpace != freeSpace) {
//            Logger.t(TAG).d("setStorageSpace: " + totalSpace + ", " + freeSpace);
            mStorageTotalSpace = totalSpace;
            mStorageFreeSpace = freeSpace;
            notifyStateChanged();
        }
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

    public int getRecordState() {
        return mRecordState;
    }

    public int getRecordMode() {
        return mRecordModeIndex;
    }

    public int getMicState() {
        return mMicState;
    }

}
