package com.waylens.hachi.service.upload.rest.response;

import com.xfdingustc.snipe.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/9/8.
 */
public class UploadDataResponse {
    public int result;//": int,
    public String jid;//    "jid": string,
    public String guid;//    "guid": string,
    public int moment_id;//    "moment_id": int,

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
