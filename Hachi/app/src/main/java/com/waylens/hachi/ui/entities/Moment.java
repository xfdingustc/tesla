package com.waylens.hachi.ui.entities;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Spannable;

import com.google.gson.annotations.Expose;
import com.waylens.hachi.utils.ToStringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;


public class Moment implements Serializable {

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

    public transient Spannable comments;

    public transient User owner;

    public int type;

    public double gpsLongitude;

    public double gpsLatitude;

    public double gpsAltitude;

    public String city;

    public String region;

    public String country;

    public static final String PROVIDER_WAYLENS = "waylens";
    public static final String PROVIDER_YOUTUBE = "youtube";
    public static final String YOUTUBE_THUMBNAIL = "https://i1.ytimg.com/vi/%s/hqdefault.jpg";

    public static Moment fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        JSONObject jsonMoment;
        if (jsonObject.has("moment")) {
            jsonMoment = jsonObject.optJSONObject("moment");
        } else {
            jsonMoment = jsonObject;
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

        JSONObject ownerInfo = jsonObject.optJSONObject("owner");
        if (ownerInfo == null) {
            moment.owner = User.fromCurrentUser();
        } else {
            moment.owner = User.fromJson(ownerInfo);
        }

        moment.parseGPSInfo(jsonMoment.optJSONObject("gps"));
        moment.parsePlace(jsonMoment.optJSONObject("place"));

        return moment;
    }

    void parseGPSInfo(JSONObject gpsInfo) {
        if (gpsInfo == null) {
            return;
        }
        if (!"Point".equals(gpsInfo.optString("type"))) {
            return;
        }
        JSONArray coordinates = gpsInfo.optJSONArray("coordinates");
        if (coordinates != null && coordinates.length() == 3) {
            gpsLongitude = coordinates.optDouble(0);
            gpsLatitude = coordinates.optDouble(1);
            gpsAltitude = coordinates.optDouble(2);
        }
    }

    void parsePlace(JSONObject placeInfo) {
        if (placeInfo == null) {
            return;
        }
        city = placeInfo.optString("city");
        region = placeInfo.optString("region");
        country = placeInfo.optString("country");
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }


}
