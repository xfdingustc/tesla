package com.waylens.hachi.ui.entities;

import com.orhanobut.logger.Logger;

import org.json.JSONObject;

/**
 * Created by Richard on 9/8/15.
 */
public class NotificationEvent {
    public static final int NOTIFICATION_TYPE_COMMENT = 0;
    public static final int NOTIFICATION_TYPE_FOLLOW = 1;
    public static final int NOTIFICATION_TYPE_LIKE = 2;
    public long eventID;
    public boolean isRead;
    public int mNotificationType;
    public long momentID;
    public String title;
    public String provider;
    public String videoID;
    public String thumbnail;
    public long time;

    NotificationEvent(JSONObject jsonObject) {
        eventID = jsonObject.optLong("eventID");
        isRead = jsonObject.optBoolean("isRead");

        JSONObject jsonMoment = jsonObject.optJSONObject("moment");
        Logger.t("Notification Event").d(jsonObject.toString());
        if (jsonMoment != null) {
            momentID = jsonMoment.optLong("momentID");
            title = jsonMoment.optString("name");
            provider = jsonMoment.optString("provider");
            if (Moment.PROVIDER_YOUTUBE.equals(provider)) {
                videoID = jsonMoment.optString("videoID");
                thumbnail = String.format(Moment.YOUTUBE_THUMBNAIL, videoID);
            } else {
                thumbnail = jsonMoment.optString("videoThumbnail");
            }
        }
    }
}
