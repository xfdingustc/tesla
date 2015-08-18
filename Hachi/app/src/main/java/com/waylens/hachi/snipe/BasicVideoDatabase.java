package com.waylens.hachi.snipe;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public class BasicVideoDatabase implements VideoDatabase {

    //public

    @Override
    public RawResponse performRequest(VdbRequest<?> vdbRequest) throws SnipeError {
        return new RawResponse(0, false);
    }
}
