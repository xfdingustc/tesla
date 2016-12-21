package com.waylens.hachi.rest.response;




import com.waylens.hachi.rest.bean.User;
import com.waylens.hachi.rest.bean.VehicleInfo;
import com.waylens.hachi.snipe.utils.ToStringUtils;
import com.waylens.hachi.ui.entities.UserDeprecated;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Xiaofei on 2016/6/12.
 */
public class MomentInfo implements Serializable {
    public MomentBasicInfo moment;

    public User owner;

    public ArrayList<RawDataUrl> rawDataurl;

    public static class MomentBasicInfo implements Serializable {
        public static final long INVALID_MOMENT_ID = -1;

        public long id;

        public String title;

        public String description;

        public String videoUrl;

        public String thumbnail;

        public String captureTime;

        public int duration;

        public int likesCount;

        public int commentsCount;

        public Map<String, String> overlay;

        public VehicleInfo momentVehicleInfo;

        public String momentType;

        public TimingInfo momentTimingInfo;

        public boolean isLiked;

        @Override
        public String toString() {
            return ToStringUtils.getString(this);
        }
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



    public static class TimingInfo implements Serializable {
        public long t1;
        public long t2;
        public long t3_1;
        public long t4_1;
        public long t5_1;
        public long t6_1;
        public long t3_2;
        public long t4_2;
        public long t5_2;
        public long t6_2;
    }
}
