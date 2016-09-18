package com.waylens.hachi.rest.response;


import com.waylens.hachi.bgjob.upload.UploadServer;
import com.xfdingustc.snipe.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/6/17.
 */
public class CreateMomentResponse {
    public long momentID;
    public UploadServer uploadServer;



    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
