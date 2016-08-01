package com.waylens.hachi.rest.response;


import com.xfdingustc.snipe.utils.ToStringUtils;

/**
 * Created by liushuwei on 2016/6/13.
 */
public class SignUpResponse {
    public User user;



    public static class User {
        public String userID;

        public String userName;

        public String avatarUrl;

        public boolean isVerified;
    }

    public String token;

    @Override
    public String toString() { return ToStringUtils.getString(this);}

}
