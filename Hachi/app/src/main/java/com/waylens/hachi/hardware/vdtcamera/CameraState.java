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
    public static final int VIDEO_RESOLUTION_1080_P_60 = 1;
    public static final int VIDEO_RESOLUTION_720_P_30 = 2;
    public static final int VIDEO_RESOLUTION_720_P_60 = 3;
    public static final int VIDEO_RESOLUTION_4_KP_30 = 4;
    public static final int VIDEO_RESOLUTION_4_KP_60 = 5;
    public static final int VIDEO_RESOLUTION_480_P_30 = 6;
    public static final int VIDEO_RESOLUTION_480_P_60 = 7;
    public static final int VIDEO_RESOLUTION_720_P_120 = 8;
    public static final int VIDEO_RESOLUTION_STILL = 9;
    public static final int Video_Resolution_Num = 10;

    public static final int VIDEO_QUALITY_UNKNOWN = -1;
    public static final int Video_Quality_Supper = 0;
    public static final int Video_Quality_HI = 1;
    public static final int Video_Quality_Mid = 2;
    public static final int Video_Quality_LOW = 3;
    public static final int Video_Quality_Num = 4;

    public static final int REC_MODE_UNKNOWN = -1;
    public static final int FLAG_AUTO_RECORD = 1 << 0;
    public static final int FLAG_LOOP_RECORD = 1 << 1;
    public static final int Rec_Mode_Manual = 0;
    public static final int REC_MODE_AUTOSTART = FLAG_AUTO_RECORD;
    public static final int Rec_Mode_Manual_LOOP = FLAG_LOOP_RECORD;
    public static final int REC_MODE_AUTOSTART_LOOP = (FLAG_AUTO_RECORD | FLAG_LOOP_RECORD);

    public static final int Color_Mode_Unknown = -1;
    public static final int Color_Mode_NORMAL = 0;
    public static final int Color_Mode_SPORT = 1;
    public static final int Color_Mode_CARDV = 2;
    public static final int Color_Mode_SCENE = 3;
    public static final int Color_Mode_Num = 4;

    public static final int Error_StartRecord_OK = 0;
    public static final int Error_StartRecord_NoCard = 1;
    public static final int Error_StartRecord_CardFull = 2;
    public static final int Error_StartRecord_CardError = 3;

    public int mStateSN = 0;
    public boolean mbSchedule = false;

    public String mCameraName = "";
    public String mFirmwareVersion = "";
    public int mApiVersion = 0;
    public String mBuild = "";

    private int mRecordState = STATE_RECORD_UNKNOWN;

    public boolean mbIsStill = false;
    public int mRecordDuration = -1;
    public boolean mbRecordDurationUpdated; //
    public long mRecordTimeFetchedTime;

    private int mMicState = STATE_MIC_UNKNOWN;
    public int mMicVol = -1; // TODO

    public int mBatteryState = STATE_BATTERY_UNKNOWN;
    public int mPowerState = STATE_POWER_UNKNOWN;
    public int mBatteryVol = -1; // TODO

    public int mStorageState = STATE_STORAGE_UNKNOWN;
    public long mStorageTotalSpace = 0;
    public long mStorageFreeSpace = 0;

    public int mOverlayFlags = -1; // TODO

    public int mVideoResolutionList = 0; // TODO
    public int mVideoResolutionIndex = VIDEO_RESOLUTION_UNKNOWN;

    public int mVideoQualityList = 0; // TODO
    public int mVideoQualityIndex = VIDEO_QUALITY_UNKNOWN;

    public int mRecordModeList = 0; // TODO
    public int mRecordModeIndex = REC_MODE_UNKNOWN;

    public int mColorModeList = 0; // TODO
    public int mColorModeIndex = Color_Mode_Unknown;

    public int mMarkBeforeTime = -1;
    public int mMarkAfterTime = -1;


    // sync our states to users
    synchronized public boolean syncStates(CameraState user) {
        if (mStateSN == user.mStateSN) {
            Logger.t(TAG).d("-- syncStates: no change ---");
            return false;
        } else {
            Logger.t(TAG).d("-- syncStates ---");
        }

        user.mStateSN = mStateSN;

        user.mCameraName = mCameraName;
        user.mFirmwareVersion = mFirmwareVersion;
        user.mApiVersion = mApiVersion;
        user.mBuild = mBuild;

        user.mRecordState = mRecordState;
        user.mbIsStill = mbIsStill;
        user.mRecordDuration = mRecordDuration;
        user.mbRecordDurationUpdated = mbRecordDurationUpdated;
        user.mRecordTimeFetchedTime = mRecordTimeFetchedTime;

        user.mMicState = mMicState;
        user.mMicVol = mMicVol;

        user.mBatteryState = mBatteryState;
        user.mPowerState = mPowerState;
        user.mBatteryVol = mBatteryVol;

        user.mStorageState = mStorageState;
        user.mStorageTotalSpace = mStorageTotalSpace;
        user.mStorageFreeSpace = mStorageFreeSpace;

        user.mOverlayFlags = mOverlayFlags;

        user.mVideoResolutionList = mVideoResolutionList;
        user.mVideoResolutionIndex = mVideoResolutionIndex;

        user.mVideoQualityList = mVideoQualityList;
        user.mVideoQualityIndex = mVideoQualityIndex;

        user.mRecordModeList = mRecordModeList;
        user.mRecordModeIndex = mRecordModeIndex;

        user.mColorModeList = mColorModeList;
        user.mColorModeIndex = mColorModeIndex;

        user.mMarkBeforeTime = mMarkBeforeTime;
        user.mMarkAfterTime = mMarkAfterTime;

        return true;
    }

    private final void stateChanged() {
        mStateSN++;
        mbSchedule = true;
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
            stateChanged();
        }
    }

    synchronized public void setFirmwareVersion(String version) {
        if (!mFirmwareVersion.equals(version)) {
            Logger.t(TAG).d("setFirmwareVersion: " + version);

            mFirmwareVersion = version;
            stateChanged();
        }
    }

    synchronized public void setApiVersion(int main, int sub, String build) {
        int version = makeVersion(main, sub);
        if (mApiVersion != version || !mBuild.equals(build)) {
            Logger.t(TAG).d("setApiVersion: " + version);
            mApiVersion = version;
            mBuild = build;
            stateChanged();
        }
    }

    synchronized public void setRecordState(int state, boolean is_still) {
        if (mRecordState != state || mbIsStill != is_still) {
            Logger.t(TAG).d("setRecordState: " + state + ", is_still: " + is_still);
            mRecordState = state;
            mbIsStill = is_still;
            stateChanged();
        }
    }

    synchronized public void setRecordDuration(int duration) {
        Logger.t(TAG).d("setRecordDuration: " + duration);
        mRecordDuration = duration;
        if (mRecordState == STATE_RECORD_RECORDING || mRecordState == STATE_RECORD_STOPPING) {
            mRecordTimeFetchedTime = SystemClock.uptimeMillis();
            mbRecordDurationUpdated = true;
        }
        stateChanged();
    }

    synchronized public void setMicState(int state, int vol) {
        if (mMicState != state || mMicVol != vol) {
            Logger.t(TAG).d("setMicState: " + state + ", " + vol);
            mMicState = state;
            mMicVol = vol;
            stateChanged();
        }
    }

    synchronized public void setPowerState(int batteryState, int powerState) {
        if (mBatteryState != batteryState || mPowerState != powerState) {
            Logger.t(TAG).d("setPowerState: " + batteryState + "," + powerState);
            mBatteryState = batteryState;
            mPowerState = powerState;
            stateChanged();
        }
    }

    synchronized public void setBatteryVol(int vol) {
        if (mBatteryVol != vol) {
            Logger.t(TAG).d("setBatteryVol: " + vol);
            mBatteryVol = vol;
            stateChanged();
        }
    }

    synchronized public void setStorageState(int state) {
        if (mStorageState != state) {
            Logger.t(TAG).d("setStorageState: " + state);
            mStorageState = state;
            stateChanged();
        }
    }

    synchronized public void setStorageSpace(long totalSpace, long freeSpace) {
        if (mStorageTotalSpace != totalSpace || mStorageFreeSpace != freeSpace) {
            Logger.t(TAG).d("setStorageSpace: " + totalSpace + ", " + freeSpace);
            mStorageTotalSpace = totalSpace;
            mStorageFreeSpace = freeSpace;
            stateChanged();
        }
    }

    synchronized public void setOverlayFlags(int flags) {
        if (mOverlayFlags != flags) {

            Logger.t(TAG).d("setOverlayFlags: " + Integer.toHexString(flags));
            mOverlayFlags = flags;
            stateChanged();
        }
    }

    synchronized public void setVideoResolutionList(int list) {
        if (mVideoResolutionList != list) {

            Logger.t(TAG).d("setVideoResolutionList: " + Integer.toHexString(list));
            mVideoResolutionList = list;
            stateChanged();
        }
    }

    synchronized public void setVideoResolution(int index) {
        if (mVideoResolutionIndex != index) {
            Logger.t(TAG).d("setVideoResolution: " + index);
            mVideoResolutionIndex = index;
            stateChanged();
        }
    }

    synchronized public void setVideoQualityList(int list) {
        if (mVideoQualityList != list) {

            Logger.t(TAG).d("setVideoQualityList: " + Integer.toHexString(list));

            mVideoQualityList = list;
            stateChanged();
        }
    }

    synchronized public void setVideoQuality(int index) {
        if (mVideoQualityIndex != index) {

            Logger.t(TAG).d("setVideoQuality: " + index);

            mVideoQualityIndex = index;
            stateChanged();
        }
    }

    synchronized public void setRecordModeList(int list) {
        if (mRecordModeList != list) {

            Logger.t(TAG).d("setRecordModeList: " + Integer.toHexString(list));
            mRecordModeList = list;
            stateChanged();
        }
    }

    synchronized public void setRecordMode(int index) {
        if (mRecordModeIndex != index) {

            Logger.t(TAG).d("setRecordMode: " + index);
            mRecordModeIndex = index;
            stateChanged();
        }
    }

    synchronized public void setColorModeList(int list) {
        if (mColorModeList != list) {

            Logger.t(TAG).d("setColorModeList: " + Integer.toHexString(list));
            mColorModeList = list;
            stateChanged();
        }
    }

    synchronized public void setColorMode(int index) {
        if (mColorModeIndex != index) {

            Logger.t(TAG).d("setColorMode: " + index);
            mColorModeIndex = index;
            stateChanged();
        }
    }

    synchronized public void setMarkTime(int before, int after) {
        if (mMarkBeforeTime != before || mMarkAfterTime != after) {
            mMarkBeforeTime = before;
            mMarkAfterTime = after;
            stateChanged();
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
