package com.waylens.hachi.ui.adapters;

import com.waylens.hachi.session.SessionManager;

import org.json.JSONObject;

/**
 * Created by Richard on 8/26/15.
 */
public class BasicUserInfo {
    public String userID;

    public String userName;

    public String avatarUrl;

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
}
