package com.waylens.hachi.ui.entities;

import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.utils.ToStringUtils;


import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Richard on 8/26/15.
 */

public class User implements Serializable {
    public String userID;

    public String userName;

    public String avatarUrl;

    public String name;

    public boolean isVerified;

    public int followersCnt;

    public int followingsCnt;

    public boolean isMyFollowing;

    public boolean isMutual;


    public static User fromJson(JSONObject jsonOwner) {
        if (jsonOwner == null) {
            return null;
        }
        User userInfo = new User();
        userInfo.userID = jsonOwner.optString("userID");
        userInfo.userName = jsonOwner.optString("userName");
        userInfo.avatarUrl = jsonOwner.optString("avatarUrl");
        return userInfo;
    }

    public static User fromCurrentUser() {
        User userInfo = new User();
        userInfo.userID = SessionManager.getInstance().getUserId();
        userInfo.userName = SessionManager.getInstance().getUserName();
        userInfo.avatarUrl = SessionManager.getInstance().getAvatarUrl();
        return userInfo;
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }

    public int getFollowersCount() {
        return followersCnt;
    }

    public int getFollowingsCount() {
        return followingsCnt;
    }

    public boolean getIsFollowing() {
        return isMyFollowing;
    }

    public void setIsFollowing(boolean isFollowing) {
        isMyFollowing = isFollowing;
    }


}
