package com.rest.response;

import com.google.gson.annotations.SerializedName;
import com.waylens.hachi.utils.ToStringUtils;

import java.util.ArrayList;
import java.util.List;

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

    public int socialProviders;


    public String facebookName;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}