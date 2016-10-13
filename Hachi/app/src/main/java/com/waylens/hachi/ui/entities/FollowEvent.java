package com.waylens.hachi.ui.entities;

import org.json.JSONObject;

/**
 * Created by lshw on 16/8/3.
 */
public class FollowEvent extends NotificationEvent {

    public boolean isMyFollowing;
    public UserDeprecated follower;
    public long createTime;

    FollowEvent(JSONObject jsonObject) {
        super(jsonObject);
        this.mNotificationType = NOTIFICATION_TYPE_FOLLOW;
    }

    public static FollowEvent fromJson(JSONObject jsonObject) {
        FollowEvent followEvent = new FollowEvent(jsonObject);
        JSONObject jsonFollow = jsonObject.optJSONObject("follow");
        if (jsonFollow != null) {
            followEvent.isMyFollowing = jsonFollow.optBoolean("isMyFollowing");
            followEvent.follower = UserDeprecated.fromJson(jsonFollow.optJSONObject("user"));
            followEvent.createTime = jsonFollow.optLong("createTime");
            followEvent.time = followEvent.createTime;
        }
        return followEvent;
    }
}
