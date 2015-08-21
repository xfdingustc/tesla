package com.waylens.hachi.ui.adapters;

import org.json.JSONObject;

/**
 * Created by Richard on 8/21/15.
 */
public class Moment {

    public int id;
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

    public Owner owner;

    private static final String PROVIDER_WAYLENS = "waylens";
    private static final String PROVIDER_YOUTUBE = "youtube";
    private static final String YOUTUBE_THUMBNAIL = "https://i1.ytimg.com/vi/%s/hqdefault.jpg";

    public static Moment fromJson(JSONObject jsonObject) {
        if (jsonObject == null || !jsonObject.has("moment")) {
            return null;
        }

        JSONObject jsonMoment = jsonObject.optJSONObject("moment");
        Moment moment = new Moment();
        moment.id = jsonMoment.optInt("id");
        String provider = jsonMoment.optString("provider");
        moment.provider = provider;
        if (PROVIDER_YOUTUBE.equals(provider)) {
            moment.thumbnail = String.format(YOUTUBE_THUMBNAIL, jsonMoment.optString("videoID"));
        } else {
            moment.thumbnail = jsonMoment.optString("thumbnail");
        }


        moment.title = jsonMoment.optString("title");
        moment.description = jsonMoment.optString("description");

        moment.captureTime = jsonMoment.optString("captureTime");
        moment.uploadTime = jsonMoment.optInt("id");
        moment.fragmentCount = jsonMoment.optInt("fragmentCount");
        moment.duration = jsonMoment.optLong("duration");
        moment.likesCount = jsonMoment.optInt("likesCount");
        moment.commentsCount = jsonMoment.optInt("commentsCount");
        moment.isLiked = jsonMoment.optBoolean("isLiked");

        moment.owner = Owner.fromJson(jsonObject.optJSONObject("owner"));

        return moment;
    }

    public static class Owner {
        public String userID;

        public String userName;

        public String avatarUrl;

        public static Owner fromJson(JSONObject jsonOwner) {
            if (jsonOwner == null) {
                return null;
            }
            Owner owner = new Owner();
            owner.userID = jsonOwner.optString("userID");
            owner.userName = jsonOwner.optString("userName");
            owner.avatarUrl = jsonOwner.optString("avatarUrl");
            return owner;
        }

    }
}
