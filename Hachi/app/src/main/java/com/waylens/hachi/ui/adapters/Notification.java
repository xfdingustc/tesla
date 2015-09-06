package com.waylens.hachi.ui.adapters;

import org.json.JSONObject;

/**
 * Created by Richard on 9/6/15.
 */
public class Notification {
    public long momentID;
    public String title;
    public String provider;
    public String videoID;
    public int commentsCount;
    public boolean hasUnread;
    public String thumbnail;

    public static Notification fromJson(JSONObject jsonObject) {
        Notification notification = new Notification();
        notification.momentID = jsonObject.optLong("momentID");
        notification.title = jsonObject.optString("title");

        notification.provider = jsonObject.optString("provider");
        notification.commentsCount = jsonObject.optInt("commentsCount");
        notification.hasUnread = jsonObject.optBoolean("hasUnread");

        if (Moment.PROVIDER_YOUTUBE.equals(notification.provider)) {
            notification.videoID = jsonObject.optString("videoID");
            notification.thumbnail = String.format(Moment.YOUTUBE_THUMBNAIL, notification.videoID);
        } else {
            notification.thumbnail = jsonObject.optString("thumbnail");
        }
        return notification;
    }

}
