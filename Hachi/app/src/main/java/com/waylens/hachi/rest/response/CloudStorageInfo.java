package com.waylens.hachi.rest.response;

import com.waylens.hachi.utils.ToStringUtils;

/**
 * Created by liushuwei on 16/7/6.
 */
public class CloudStorageInfo {

    public DurationQuota current;

    public static class DurationQuota {
        public int durationUsed;
        public DurationDetail plan;
    }

    public static class DurationDetail {
        public int durationQuota;
        public long cycleBegin;
        public long cycleEnd;
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
