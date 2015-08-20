package com.waylens.hachi.snipe;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public interface VdbSocket {
    RawResponse performRequest(VdbRequest<?> vdbRequest) throws SnipeError;
}
