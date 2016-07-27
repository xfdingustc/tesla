package com.waylens.hachi.bgjob.social.event;

import java.util.Objects;

/**
 * Created by Xiaofei on 2016/7/27.
 */
public class SocialEvent {

    public static final int EVENT_WHAT_REPOST = 0;

    private final int mWhat;
    private final boolean mResult;
    private final Object mExtra;

    public SocialEvent(int what, boolean result, Object extra) {
        this.mWhat = what;
        this.mResult = result;
        this.mExtra = extra;
    }

    public int getWhat() {
        return mWhat;
    }

    public boolean getResult() {
        return mResult;
    }

    public Object getExtra() {
        return mExtra;
    }
}
