package com.waylens.hachi.ui.entities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.waylens.hachi.session.SessionManager;
import com.xfdingustc.snipe.utils.ToStringUtils;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Richard on 8/26/15.
 */

public class User implements Serializable {
    @Expose
    public String userID;

    @Expose
    public String userName;

    @Expose
    public String avatarUrl;

    @Expose
    public String name;

    @Expose
    @SerializedName("followersCnt")
    private int mFollowersCount;

    @Expose
    @SerializedName("followingsCnt")
    private int mFollowingsCount;

    @Expose
    @SerializedName("isMyFollowing")
    private boolean mIsFollowing;

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
        return mFollowersCount;
    }

    public int getFollowingsCount() {
        return mFollowingsCount;
    }

    public boolean getIsFollowing() {
        return mIsFollowing;
    }

    public void setIsFollowing(boolean isFollowing) {
        mIsFollowing = isFollowing;
    }


    public static List<User> parseUserListFromJson(JSONObject response) {
        try {
            JSONArray userArray = response.getJSONArray("friends");
            List<User> userList;

            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

            Type type = new TypeToken<ArrayList<User>>() {}.getType();

            userList = gson.fromJson(userArray.toString(), type);


            return userList;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
}
