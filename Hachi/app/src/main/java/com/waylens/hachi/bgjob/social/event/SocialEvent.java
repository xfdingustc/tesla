package com.waylens.hachi.bgjob.social.event;

import com.waylens.hachi.rest.response.RepostResponse;

import java.util.Objects;

/**
 * Created by Xiaofei on 2016/7/27.
 */
public class SocialEvent {

    public static final int EVENT_WHAT_REPOST = 0;

    private final int mWhat;
    private final RepostResponse mResponse;
    private final Object mExtra;

    public SocialEvent(int what, RepostResponse response, Object extra) {
        this.mWhat = what;
        this.mResponse = response;
        this.mExtra = extra;
    }

    public int getWhat() {
        return mWhat;
    }

    public RepostResponse getResponse() {
        return mResponse;
    }

    public Object getExtra() {
        return mExtra;
    }
}
