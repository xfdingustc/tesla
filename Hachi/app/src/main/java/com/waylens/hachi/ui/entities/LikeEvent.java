package com.waylens.hachi.ui.entities;

import org.json.JSONObject;

/**
 * Created by Richard on 9/8/15.
 */
public class LikeEvent extends NotificationEvent {

    public long likeID;
    public User liker;
    public long createTime;

    LikeEvent(JSONObject jsonObject) {
        super(jsonObject);
    }

    public static LikeEvent fromJson(JSONObject jsonObject) {
        LikeEvent likeEvent = new LikeEvent(jsonObject);
        JSONObject jsonLike = jsonObject.optJSONObject("like");
        if (jsonLike != null) {
            likeEvent.likeID = jsonLike.optLong("likeID");
            likeEvent.liker = User.fromJson(jsonLike.optJSONObject("user"));
            likeEvent.createTime = jsonLike.optLong("createTime");
        }
        return likeEvent;
    }
}
