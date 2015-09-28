package com.waylens.hachi.ui.entities;

import android.text.Spannable;

import com.google.gson.annotations.Expose;
import com.waylens.hachi.utils.ToStringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Richard on 8/21/15.
 */
public class Moment {

    public static final long INVALID_MOMENT_ID = -1;

    public static final int TYPE_WAYLENS = 0;
    public static final int TYPE_YOUTUBE = 1;

    @Expose
    public long id;

    @Expose
    public String provider;

    @Expose
    public String title;

    @Expose
    public String description;

    @Expose
    public String thumbnail;

    @Expose
    public String captureTime;

    @Expose
    public long uploadTime;

    @Expose
    public int fragmentCount;

    @Expose
    public long duration;

    @Expose
    public int likesCount;

    @Expose
    public int commentsCount;

    @Expose
    public boolean isLiked;

    @Expose
    public String videoID;

    @Expose
    public String videoURL;

    public Spannable comments;

    public User owner;

    public int type;

    public static final String PROVIDER_WAYLENS = "waylens";
    public static final String PROVIDER_YOUTUBE = "youtube";
    public static final String YOUTUBE_THUMBNAIL = "https://i1.ytimg.com/vi/%s/hqdefault.jpg";

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
            moment.videoURL = jsonMoment.optString("videoUrl");
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

        moment.owner = User.fromJson(jsonObject.optJSONObject("owner"));

        return moment;
    }

    public static Moment fromMyMoment(JSONObject jsonMoment) {
        if (jsonMoment == null) {
            return null;
        }

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
        moment.owner = User.fromCurrentUser();

        return moment;
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
