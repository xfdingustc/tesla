package com.waylens.hachi.ui.entities;

import com.google.gson.annotations.Expose;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.utils.ToStringUtils;

import org.json.JSONObject;

/**
 * Created by Richard on 8/26/15.
 */

public class BasicUserInfo {
    @Expose
    public String userID = null;

    @Expose
    public String userName = null;

    @Expose
    public String avatarUrl = null;

    @Expose
    public String name = null;

    public static BasicUserInfo fromJson(JSONObject jsonOwner) {
        if (jsonOwner == null) {
            return null;
        }
        BasicUserInfo userInfo = new BasicUserInfo();
        userInfo.userID = jsonOwner.optString("userID");
        userInfo.userName = jsonOwner.optString("userName");
        userInfo.avatarUrl = jsonOwner.optString("avatarUrl");
        return userInfo;
    }

    public static BasicUserInfo fromCurrentUser() {
        BasicUserInfo userInfo = new BasicUserInfo();
        userInfo.userID = SessionManager.getInstance().getUserId();
        userInfo.userName = SessionManager.getInstance().getUserName();
        userInfo.avatarUrl = SessionManager.getInstance().getAvatarUrl();
        return userInfo;
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
