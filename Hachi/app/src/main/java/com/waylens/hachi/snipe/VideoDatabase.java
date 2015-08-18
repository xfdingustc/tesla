package com.waylens.hachi.snipe;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public interface VideoDatabase {
    RawResponse performRequest(VdbRequest<?> vdbRequest) throws SnipeError;
}
