package com.waylens.hachi.ui.clips.event;

/**
 * Created by lshw on 16/11/10.
 */

public class ActionButtonEvent {
    public static final int FAB_SMART_REMIX = 0;
    public final Object mExtra;
    public final int mWhat;
    public ActionButtonEvent(int what, Object extra) {
        mWhat = what;
        mExtra = extra;
    }
}
