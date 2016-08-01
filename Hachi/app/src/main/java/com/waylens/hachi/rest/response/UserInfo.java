package com.waylens.hachi.rest.response;


import com.xfdingustc.snipe.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/6/13.
 */
public class UserInfo {
    public String userName;

    public String displayName;

    public String email;

    public String gender;

    public String birthday;

    public String region;

    public boolean isVerified;

    public String avatarUrl;

    public String avatarThumbnailUrl;

    public int socialProviders;

    public String facebookName;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
