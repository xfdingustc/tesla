package com.waylens.hachi.bgjob;

import android.support.annotation.NonNull;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService;

/**
 * Created by Xiaofei on 2016/4/28.
 */
public class BgJobService extends FrameworkJobSchedulerService {
    @NonNull
    @Override
    protected JobManager getJobManager() {
        return BgJobManager.getManager();
    }
}
