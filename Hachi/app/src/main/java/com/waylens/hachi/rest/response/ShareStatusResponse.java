package com.waylens.hachi.rest.response;

import java.util.List;

/**
 * Created by lshw on 17/1/5.
 */

public class ShareStatusResponse {
    
    public List<ShareStatus> shareStatuses;

    public static class ShareStatus {
        public String provider;
        public Long createTime;
        public String shareStatus;
        public String videoID;
        public String finishTime;
    }
}
