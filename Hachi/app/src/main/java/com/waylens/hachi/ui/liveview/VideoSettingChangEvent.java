package com.waylens.hachi.ui.liveview;

/**
 * Created by Xiaofei on 2016/5/29.
 */
public class VideoSettingChangEvent {
    private final int mWhat;
    private final int mValue;

    public static final int WHAT_FRAMERATE = 0;
    public static final int WHAT_RESOLUTION = 1;

    public VideoSettingChangEvent(int what, int value) {
        this.mWhat = what;
        this.mValue = value;
    }

    public int getWhat() {
        return mWhat;
    }

    public int getValue() {
        return mValue;
    }
}

