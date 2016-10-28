package com.waylens.hachi.ui.entities.moment;

import android.text.TextUtils;

import com.waylens.hachi.rest.bean.MomentTimingInfo;
import com.waylens.hachi.rest.bean.VehicleInfo;
import com.waylens.hachi.snipe.utils.ToStringUtils;


import java.io.Serializable;
import java.util.List;

/**
 * Created by Xiaofei on 2016/9/13.
 */
public class MomentAbstract implements Serializable{
    public long id;
    public String title;
    public String description;
    public List<String> hashTags;
    public String videoUrl;
    public String thumbnail;
    public String captureTime;
    public long uploadTime;
    public long createTime;
    public int duration;

    public Gps gps;
    public boolean withGeoTag;

    public PlaceInfo place;

    public String accessLevel;

    public int likesCount;
    public int commentsCount;

    public boolean isLiked;

    public boolean isRecommended;

    public String momentType;

    public VehicleInfo momentVehicleInfo;

    public MomentTimingInfo momentTimingInfo;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }

    public boolean isRacingMoment() {
        return !TextUtils.isEmpty(momentType) && momentType.startsWith("RACING");

    }

    public String getRaceType() {
        return momentTimingInfo.getRaceType(momentType);
    }

    public String getRaceTime() {
        return momentTimingInfo.getRaceTime(momentType);
    }

    public boolean isPictureMoment() {
        return !TextUtils.isEmpty(momentType) && momentType.equals("PICTURE");
    }
}
