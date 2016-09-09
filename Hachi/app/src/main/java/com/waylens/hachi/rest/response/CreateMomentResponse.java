package com.waylens.hachi.rest.response;


import com.xfdingustc.snipe.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/6/17.
 */
public class CreateMomentResponse {
    public long momentID;
    public UploadServer uploadServer;

    public static class UploadServer {
        public String ip;
        public int port;
        public String privateKey;
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
