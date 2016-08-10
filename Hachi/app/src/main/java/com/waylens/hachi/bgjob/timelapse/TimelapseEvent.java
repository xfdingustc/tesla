package com.waylens.hachi.bgjob.timelapse;

/**
 * Created by Xiaofei on 2016/8/11.
 */
public class TimelapseEvent {
    public static final int EVENT_START = 0;
    public static final int EVENT_PROGRESS = 1;
    public static final int EVENT_END = 2;
    private final int mWhat;
    private final Object mExtra;

    public TimelapseEvent(int what) {
        this(what, null);
    }

    public TimelapseEvent(int what, Object extra) {
        this.mWhat = what;
        this.mExtra = extra;
    }

    public int getWhat() {
        return mWhat;
    }

    public Object getExtra() {
        return mExtra;
    }
}
