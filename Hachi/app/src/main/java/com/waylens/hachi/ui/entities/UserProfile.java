package com.waylens.hachi.ui.entities;


import com.waylens.hachi.snipe.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/6/8.
 */
public class UserProfile {

    public String userName;

    public String displayName;

    public String email;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
