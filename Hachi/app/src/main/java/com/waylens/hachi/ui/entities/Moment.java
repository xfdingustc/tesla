package com.waylens.hachi.ui.entities;

import android.graphics.Picture;
import android.text.Spannable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.rest.response.MomentInfo;
import com.xfdingustc.snipe.utils.ToStringUtils;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Moment implements Serializable {
    private static final String TAG = Moment.class.getSimpleName();
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

    public User owner;

    public int type;

    public double gpsLongitude;

    public double gpsLatitude;

    public double gpsAltitude;

    public String city;

    public String region;

    public String country;

    public MomentInfo.VehicleInfo momentVehicleInfo;

    public MomentInfo.TimingInfo momentTimingInfo;

    public String momentType;

    public List<MomentPicture> pictureUrls;

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
        moment.momentType = jsonMoment.optString("momentType");

        JSONObject ownerInfo = jsonObject.optJSONObject("owner");
        if (ownerInfo == null) {
            moment.owner = User.fromCurrentUser();
        } else {
            moment.owner = User.fromJson(ownerInfo);
        }

        moment.parseGPSInfo(jsonMoment.optJSONObject("gps"));
        moment.parsePlace(jsonMoment.optJSONObject("place"));

        moment.pictureUrls = new ArrayList<>();
        JSONArray pictures = jsonObject.optJSONArray("pictureUrls");
        if (pictures != null) {
          for (int i = 0; i < pictures.length(); i++) {
              JSONObject onePicture = pictures.optJSONObject(i);
              if (onePicture != null) {
                  MomentPicture oneMomentPicture = new MomentPicture();
                  oneMomentPicture.pictureID = onePicture.optLong("pictureID");
                  oneMomentPicture.original = onePicture.optString("original");
                  oneMomentPicture.smallThumbnail = onePicture.optString("smallThumbnail");
                  oneMomentPicture.bigThumbnail = onePicture.optString("bigThumbnail");
                  moment.pictureUrls.add(oneMomentPicture);
              }
          }
        }

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

    public static List<Moment> parseMomentArray(JSONObject response) {
        ArrayList<Moment> moments = new ArrayList<>();
        try {
            JSONArray momentArray = response.getJSONArray(JsonKey.MOMENTS);
            for (int i = 0; i < momentArray.length(); i++) {
                JSONObject momentObject = momentArray.getJSONObject(i);
                Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
                Moment moment = gson.fromJson(momentObject.toString(), Moment.class);
                moment.owner = new User();
//                moment.owner.userID = mUserID;
//                moment.owner.avatarUrl = mUserInfo.avatarUrl;
                moments.add(moment);

//                Logger.t(TAG).d("Add one moment: " + moment.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return moments;
    }


    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
