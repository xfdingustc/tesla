package com.waylens.hachi.eventbus.events;

/**
 * Created by Xiaofei on 2016/4/6.
 */
public class GaugeEvent {
    private final int mWhat;
    private final Object mExtra;

    public static final int EVENT_WHAT_SHOW = 0;
    public static final int EVENT_WHAT_UPDATE_SETTING = 1;
    public static final int EVENT_WHAT_CHANGE_THEME = 2;

    public GaugeEvent(int what, Object extra) {
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
