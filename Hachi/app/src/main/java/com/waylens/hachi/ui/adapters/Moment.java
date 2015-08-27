package com.waylens.hachi.ui.adapters;

import android.text.Spannable;

import org.json.JSONObject;

/**
 * Created by Richard on 8/21/15.
 */
public class Moment {

    public static final long INVALID_MOMENT_ID = -1;

    public static final int TYPE_WAYLENS = 0;
    public static final int TYPE_YOUTUBE = 1;

    public long id;
    public String provider;
    public String title;
    public String description;
    public String thumbnail;
    public String captureTime;
    public long uploadTime;
    public int fragmentCount;
    public long duration;
    public int likesCount;
    public int commentsCount;
    public boolean isLiked;
    public String videoID;

    public Spannable comments;

    public BasicUserInfo owner;

    public int type;

    private static final String PROVIDER_WAYLENS = "waylens";
    private static final String PROVIDER_YOUTUBE = "youtube";
    private static final String YOUTUBE_THUMBNAIL = "https://i1.ytimg.com/vi/%s/hqdefault.jpg";

    public static Moment fromJson(JSONObject jsonObject) {
        if (jsonObject == null || !jsonObject.has("moment")) {
            return null;
        }

        JSONObject jsonMoment = jsonObject.optJSONObject("moment");
        Moment moment = new Moment();
        moment.id = jsonMoment.optLong("id");
        String provider = jsonMoment.optString("provider");
        moment.provider = provider;

        if (PROVIDER_YOUTUBE.equals(provider)) {
            moment.videoID = jsonMoment.optString("videoID");
            moment.thumbnail = String.format(YOUTUBE_THUMBNAIL, moment.videoID);
            moment.type = TYPE_YOUTUBE;
        } else {
            moment.thumbnail = jsonMoment.optString("thumbnail");
            moment.type = TYPE_WAYLENS;
        }


        moment.title = jsonMoment.optString("title");
        moment.description = jsonMoment.optString("description");

        moment.captureTime = jsonMoment.optString("captureTime");
        moment.uploadTime = jsonMoment.optLong("uploadTime");
        moment.fragmentCount = jsonMoment.optInt("fragmentCount");
        moment.duration = jsonMoment.optLong("duration");
        moment.likesCount = jsonMoment.optInt("likesCount");
        moment.commentsCount = jsonMoment.optInt("commentsCount");
        moment.isLiked = jsonMoment.optBoolean("isLiked");

        moment.owner = BasicUserInfo.fromJson(jsonObject.optJSONObject("owner"));

        return moment;
    }

}
