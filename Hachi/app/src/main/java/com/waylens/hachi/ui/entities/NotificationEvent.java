package com.waylens.hachi.ui.entities;

import org.json.JSONObject;

/**
 * Created by Richard on 9/8/15.
 */
public class NotificationEvent {
    public long eventID;
    public boolean isRead;

    public long momentID;
    public String title;
    public String provider;
    public String videoID;
    public String thumbnail;

    NotificationEvent(JSONObject jsonObject) {
        eventID = jsonObject.optLong("eventID");
        isRead = jsonObject.optBoolean("isRead");

        JSONObject jsonMoment = jsonObject.optJSONObject("moment");
        if (jsonMoment != null) {
            momentID = jsonMoment.optLong("momentID");
            title = jsonMoment.optString("title");
            provider = jsonMoment.optString("provider");
            if (Moment.PROVIDER_YOUTUBE.equals(provider)) {
                videoID = jsonMoment.optString("videoID");
                thumbnail = String.format(Moment.YOUTUBE_THUMBNAIL, videoID);
            } else {
                thumbnail = jsonMoment.optString("thumbnail");
            }
        }
    }
}
