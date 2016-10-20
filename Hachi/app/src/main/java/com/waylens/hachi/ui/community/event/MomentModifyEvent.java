package com.waylens.hachi.ui.community.event;

/**
 * Created by laina on 16/10/21.
 */

public class MomentModifyEvent {
    public static String TAG = MomentModifyEvent.class.getSimpleName();
    public static final int LIKE_EVENT = 0x0001;
    public static final int COMMENT_EVENT = 0x0002;
    public static final int DELETE_EVENT = 0x0003;
    public static final int EDIT_EVENT = 0x0004;

    public int eventType;

    public int momentIndex;

    public Object what;

    public MomentModifyEvent(int eventType, int momentIndex, Object what) {
        this.eventType = eventType;
        this.momentIndex = momentIndex;
        this.what = what;
    }

    public MomentModifyEvent(int eventType) {
        this(eventType, -1, null);
    }

}
