package com.waylens.hachi.rest.response;



import com.xfdingustc.snipe.utils.ToStringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Xiaofei on 2016/6/12.
 */
public class MomentInfo implements Serializable {
    public MomentBasicInfo moment;

    public Owner owner;

    public ArrayList<RawDataUrl> rawDataurl;

    public static class MomentBasicInfo implements Serializable {
        public long id;

        public String title;

        public String description;

        public String videoUrl;

        public String thumbnail;

        public int duration;

        public int likesCount;

        public int commentsCount;

        public Map<String, String> overlay;

        public boolean isLiked;
    }

    public static class Owner implements Serializable {
        public String userID;

        public String userName;

        public String avatarUrl;

        public boolean isVerified;
    }

    public static class RawDataUrl implements Serializable {
        public String captureTime;

        public int duration;

        public String url;
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
