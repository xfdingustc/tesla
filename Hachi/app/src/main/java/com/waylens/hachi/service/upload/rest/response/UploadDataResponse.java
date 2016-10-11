package com.waylens.hachi.service.upload.rest.response;


import com.waylens.hachi.snipe.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/9/8.
 */
public class UploadDataResponse {
    public int result;
    public String jid;
    public String guid;
    public int moment_id;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
