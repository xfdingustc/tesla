package com.waylens.hachi.ui.entities.moment;

import com.xfdingustc.snipe.utils.ToStringUtils;

import java.util.List;

/**
 * Created by Xiaofei on 2016/9/13.
 */
public class MomentAbstract {
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

    public Place place;

    public String accessLevel;

    public int likesCount;
    public int commentsCount;

    public boolean isLiked;

    public boolean isRecommended;

    public String momentType;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
