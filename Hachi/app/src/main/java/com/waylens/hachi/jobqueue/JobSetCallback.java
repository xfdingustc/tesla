package com.waylens.hachi.jobqueue;

import java.util.Set;

/**
 * Created by lshw on 16/11/9.
 */

public interface JobSetCallback {
    void onResult(Set<JobHolder> result);
    interface MessageWithCallback {
        void setCallback(JobSetCallback jobSetCallback);
    }
}
