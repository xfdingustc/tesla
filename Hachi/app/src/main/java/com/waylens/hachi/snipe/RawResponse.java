package com.waylens.hachi.snipe;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public class RawResponse {

    public final int statusCode;
    public final boolean notModified;

    public RawResponse(int statusCode, boolean notModified) {
        this.statusCode = statusCode;
        this.notModified = notModified;
    }




}
