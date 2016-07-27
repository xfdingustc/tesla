package com.waylens.hachi.rest.response;

import com.waylens.hachi.library.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/7/27.
 */
public class RepostResponse {
    public boolean result;
    public String shareStatus;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
