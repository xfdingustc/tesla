package com.waylens.hachi.ui.community;

import com.waylens.hachi.rest.body.MomentUpdateBody;

/**
 * Created by Xiaofei on 2016/8/10.
 */
public class MomentChangeEvent {
    private final long mMomentId;
    private final MomentUpdateBody mMomentUpdateBody;

    public MomentChangeEvent(long momentId, MomentUpdateBody momentUpdateBody) {
        this.mMomentId = momentId;
        this.mMomentUpdateBody = momentUpdateBody;
    }

    public long getMomentId() {
        return mMomentId;
    }

    public MomentUpdateBody getMomentUpdateBody() {
        return mMomentUpdateBody;
    }
}
