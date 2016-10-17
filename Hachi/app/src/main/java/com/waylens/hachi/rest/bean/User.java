package com.waylens.hachi.rest.bean;

import java.io.Serializable;

/**
 * Created by Xiaofei on 2016/10/12.
 */

public class User implements Serializable{
    public String userID;

    public String userName;

    public String avatarUrl;

    public boolean isVerified;
}
