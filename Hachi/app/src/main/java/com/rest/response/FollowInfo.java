package com.rest.response;

import com.waylens.hachi.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/6/12.
 */
public class FollowInfo {
    public int followers;

    public int followings;

    public boolean isMyFollowing;

    public boolean isMyFollower;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}