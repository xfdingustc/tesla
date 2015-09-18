package com.transee.ccam;

import android.os.SystemClock;
import android.util.Log;

import java.util.Locale;

public class CameraState {

    public static final boolean DEBUG = false;
    public static final String TAG = "CameraState";

    public static final int State_Record_Unknown = -1;
    public static final int State_Record_Stopped = 0;
    public static final int State_Record_Stopping = 1;
    public static final int State_Record_Starting = 2;
    public static final int State_Record_Recording = 3;
    public static final int State_Record_Switching = 4;

    public static final int State_Mic_Unknown = -1;
    public static final int State_Mic_ON = 0;
    public static final int State_Mic_MUTE = 1;

    public static final int State_Battery_Unknown = -1;
    public static final int State_Battery_Full = 0;
    public static final int State_Battery_NotCharging = 1;
    public static final int State_Battery_Discharging = 2;
    public static final int State_Battery_Charging = 3;

    public static final int State_Power_Unknown = -1;
    public static final int State_Power_No = 0;
    public static final int State_Power_Yes = 1;

    public static final int State_Storage_Unknown = -1;
    public static final int State_storage_noStorage = 0;
    public static final int State_storage_loading = 1;
    public static final int State_storage_ready = 2;
    public static final int State_storage_error = 3;
    public static final int State_storage_usbdisc = 4;

    public static final int OVERLAY_FLAG_NAME = 0x01;
    public static final int OVERLAY_FLAG_TIME = 0x02;
    public static final int OVERLAY_FLAG_GPS = 0x04;
    public static final int OVERLAY_FLAG_SPEED = 0x08;

    public static final int Video_Resolution_Unknown = -1;
    public static final int Video_Resolution_1080p30 = 0;
    public static final int Video_Resolution_1080p60 = 1;
    public static final int Video_Resolution_720p30 = 2;
    public static final int Video_Resolution_720p60 = 3;
    public static final int Video_Resolution_4Kp30 = 4;
    public static final int Video_Resolution_4Kp60 = 5;
    public static final int Video_Resolution_480p30 = 6;
    public static final int Video_Resolution_480p60 = 7;
    public static final int Video_Resolution_720p120 = 8;
    public static final int Video_Resolution_Still = 9;
    public static final int Video_Resolution_Num = 10;

    public static final int Video_Quality_Unknown = -1;
    public static final int Video_Quality_Supper = 0;
    public static final int Video_Quality_HI = 1;
    public static final int Video_Quality_Mid = 2;
    public static final int Video_Quality_LOW = 3;
    public static final int Video_Quality_Num = 4;

    public static final int Rec_Mode_Unknown = -1;
    public static final int FLAG_AUTO_RECORD = 0x01;
    public static final int FLAG_LOOP_RECORD = 0x02;
    public static final int Rec_Mode_Manual = 0;
    public static final int Rec_Mode_AutoStart = FLAG_AUTO_RECORD;
    public static final int Rec_Mode_Manual_circle = FLAG_LOOP_RECORD;
    public static final int Rec_Mode_AutoStart_circle = (FLAG_AUTO_RECORD | FLAG_LOOP_RECORD);

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

    public int mRecordState = State_Record_Unknown;
    public boolean mbIsStill = false;
    public int mRecordDuration = -1;
    public boolean mbRecordDurationUpdated; //
    public long mRecordTimeFetchedTime;

    public int mMicState = State_Mic_Unknown;
    public int mMicVol = -1; // TODO

    public int mBatteryState = State_Battery_Unknown;
    public int mPowerState = State_Power_Unknown;
    public int mBatteryVol = -1; // TODO

    public int mStorageState = State_Storage_Unknown;
    public long mStorageTotalSpace = 0;
    public long mStorageFreeSpace = 0;

    public int mOverlayFlags = -1; // TODO

    public int mVideoResolutionList = 0; // TODO
    public int mVideoResolutionIndex = Video_Resolution_Unknown;

    public int mVideoQualityList = 0; // TODO
    public int mVideoQualityIndex = Video_Quality_Unknown;

    public int mRecordModeList = 0; // TODO
    public int mRecordModeIndex = Rec_Mode_Unknown;

    public int mColorModeList = 0; // TODO
    public int mColorModeIndex = Color_Mode_Unknown;

    public int mMarkBeforeTime = -1;
    public int mMarkAfterTime = -1;

    // default states when not available
    public static final CameraState nullState = new CameraState();

    // sync our states to users
    synchronized public boolean syncStates(CameraState user) {
        if (mStateSN == user.mStateSN) {
            if (DEBUG) {
                Log.d(TAG, "-- syncStates: no change ---");
            }
            return false;
        } else {
            if (DEBUG) {
                Log.d(TAG, "-- syncStates ---");
            }
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
        return (mVideoResolutionList & (1 << Video_Resolution_Still)) != 0;
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
            if (DEBUG) {
                Log.d(TAG, "setCameraName: " + name);
            }
            mCameraName = name;
            stateChanged();
        }
    }

    synchronized public void setFirmwareVersion(String version) {
        if (!mFirmwareVersion.equals(version)) {
            if (DEBUG) {
                Log.d(TAG, "setFirmwareVersion: " + version);
            }
            mFirmwareVersion = version;
            stateChanged();
        }
    }

    synchronized public void setApiVersion(int main, int sub, String build) {
        int version = makeVersion(main, sub);
        if (mApiVersion != version || !mBuild.equals(build)) {
            if (DEBUG) {
                Log.d(TAG, "setApiVersion: " + version);
            }
            mApiVersion = version;
            mBuild = build;
            stateChanged();
        }
    }

    synchronized public void setRecordState(int state, boolean is_still) {
        if (mRecordState != state || mbIsStill != is_still) {
            if (DEBUG) {
                Log.d(TAG, "setRecordState: " + state + ", is_still: " + is_still);
            }
            mRecordState = state;
            mbIsStill = is_still;
            stateChanged();
        }
    }

    synchronized public void setRecordDuration(int duration) {
        if (DEBUG) {
            Log.d(TAG, "setRecordDuration: " + duration);
        }
        mRecordDuration = duration;
        if (mRecordState == State_Record_Recording || mRecordState == State_Record_Stopping) {
            mRecordTimeFetchedTime = SystemClock.uptimeMillis();
            mbRecordDurationUpdated = true;
        }
        stateChanged();
    }

    synchronized public void setMicState(int state, int vol) {
        if (mMicState != state || mMicVol != vol) {
            if (DEBUG) {
                Log.d(TAG, "setMicState: " + state + ", " + vol);
            }
            mMicState = state;
            mMicVol = vol;
            stateChanged();
        }
    }

    synchronized public void setPowerState(int batteryState, int powerState) {
        if (mBatteryState != batteryState || mPowerState != powerState) {
            if (DEBUG) {
                Log.d(TAG, "setPowerState: " + batteryState + "," + powerState);
            }
            mBatteryState = batteryState;
            mPowerState = powerState;
            stateChanged();
        }
    }

    synchronized public void setBatteryVol(int vol) {
        if (mBatteryVol != vol) {
            if (DEBUG) {
                Log.d(TAG, "setBatteryVol: " + vol);
            }
            mBatteryVol = vol;
            stateChanged();
        }
    }

    synchronized public void setStorageState(int state) {
        if (mStorageState != state) {
            if (DEBUG) {
                Log.d(TAG, "setStorageState: " + state);
            }
            mStorageState = state;
            stateChanged();
        }
    }

    synchronized public void setStorageSpace(long totalSpace, long freeSpace) {
        if (mStorageTotalSpace != totalSpace || mStorageFreeSpace != freeSpace) {
            if (DEBUG) {
                Log.d(TAG, "setStorageSpace: " + totalSpace + ", " + freeSpace);
            }
            mStorageTotalSpace = totalSpace;
            mStorageFreeSpace = freeSpace;
            stateChanged();
        }
    }

    synchronized public void setOverlayFlags(int flags) {
        if (mOverlayFlags != flags) {
            if (DEBUG) {
                Log.d(TAG, "setOverlayFlags: " + Integer.toHexString(flags));
            }
            mOverlayFlags = flags;
            stateChanged();
        }
    }

    synchronized public void setVideoResolutionList(int list) {
        if (mVideoResolutionList != list) {
            if (DEBUG) {
                Log.d(TAG, "setVideoResolutionList: " + Integer.toHexString(list));
            }
            mVideoResolutionList = list;
            stateChanged();
        }
    }

    synchronized public void setVideoResolution(int index) {
        if (mVideoResolutionIndex != index) {
            if (DEBUG) {
                Log.d(TAG, "setVideoResolution: " + index);
            }
            mVideoResolutionIndex = index;
            stateChanged();
        }
    }

    synchronized public void setVideoQualityList(int list) {
        if (mVideoQualityList != list) {
            if (DEBUG) {
                Log.d(TAG, "setVideoQualityList: " + Integer.toHexString(list));
            }
            mVideoQualityList = list;
            stateChanged();
        }
    }

    synchronized public void setVideoQuality(int index) {
        if (mVideoQualityIndex != index) {
            if (DEBUG) {
                Log.d(TAG, "setVideoQuality: " + index);
            }
            mVideoQualityIndex = index;
            stateChanged();
        }
    }

    synchronized public void setRecordModeList(int list) {
        if (mRecordModeList != list) {
            if (DEBUG) {
                Log.d(TAG, "setRecordModeList: " + Integer.toHexString(list));
            }
            mRecordModeList = list;
            stateChanged();
        }
    }

    synchronized public void setRecordMode(int index) {
        if (mRecordModeIndex != index) {
            if (DEBUG) {
                Log.d(TAG, "setRecordMode: " + index);
            }
            mRecordModeIndex = index;
            stateChanged();
        }
    }

    synchronized public void setColorModeList(int list) {
        if (mColorModeList != list) {
            if (DEBUG) {
                Log.d(TAG, "setColorModeList: " + Integer.toHexString(list));
            }
            mColorModeList = list;
            stateChanged();
        }
    }

    synchronized public void setColorMode(int index) {
        if (mColorModeIndex != index) {
            if (DEBUG) {
                Log.d(TAG, "setColorMode: " + index);
            }
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

}
