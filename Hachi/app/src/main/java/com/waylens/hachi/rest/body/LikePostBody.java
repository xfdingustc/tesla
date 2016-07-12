package com.waylens.hachi.rest.body;

/**
 * Created by Xiaofei on 2016/6/8.
 */
public class LikePostBody {
    public long momentID;
    public boolean cancel;

    public LikePostBody(long momentId, boolean cancel) {
        this.momentID = momentId;
        this.cancel = cancel;
    }
}
