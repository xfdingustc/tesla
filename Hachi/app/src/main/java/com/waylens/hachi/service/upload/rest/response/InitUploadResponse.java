package com.waylens.hachi.service.upload.rest.response;

import com.xfdingustc.snipe.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/9/7.
 */
public class InitUploadResponse {
    public int result;
    public String jid;
    public int moment_id;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
