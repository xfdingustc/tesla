package com.waylens.hachi.ui.clips.event;

/**
 * Created by lshw on 16/11/10.
 */

public class ToggleFabEvent {
    public static final int FAB_VISIBLE = 0;
    public static final int FAB_INVISIBLE = 1;
    public final Object mExtra;
    public final int mWhat;
    public ToggleFabEvent(int what, Object extra) {
        mWhat = what;
        mExtra = extra;
    }
}