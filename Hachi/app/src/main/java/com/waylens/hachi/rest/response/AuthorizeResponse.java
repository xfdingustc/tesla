package com.waylens.hachi.rest.response;


import com.waylens.hachi.rest.bean.User;
import com.waylens.hachi.snipe.utils.ToStringUtils;

/**
 * Created by liushuwei on 2016/6/13.
 */
public class AuthorizeResponse {
    public User user;


    public String token;

    public boolean isLinked;

    @Override
    public String toString() { return ToStringUtils.getString(this);}

}
