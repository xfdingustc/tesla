package com.waylens.hachi.ui.entities;

import android.text.Spannable;

import com.google.gson.annotations.Expose;
import com.waylens.hachi.rest.bean.VehicleInfo;
import com.waylens.hachi.rest.response.MomentInfo;
import com.waylens.hachi.snipe.utils.ToStringUtils;

import java.io.Serializable;
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

    public UserDeprecated owner;

    public int type;

    public double gpsLongitude;

    public double gpsLatitude;

    public double gpsAltitude;

    public String city;

    public String region;

    public String country;

    public VehicleInfo momentVehicleInfo;

    public MomentInfo.TimingInfo momentTimingInfo;

    public String momentType;

    public List<MomentPicture> pictureUrls;

    public static final String PROVIDER_WAYLENS = "waylens";
    public static final String PROVIDER_YOUTUBE = "youtube";
    public static final String YOUTUBE_THUMBNAIL = "https://i1.ytimg.com/vi/%s/hqdefault.jpg";

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
