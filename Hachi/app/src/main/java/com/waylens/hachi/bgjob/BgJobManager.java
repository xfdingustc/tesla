package com.waylens.hachi.bgjob;

import android.content.Context;
import android.os.Build;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.birbit.android.jobqueue.log.CustomLogger;
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService;

/**
 * Created by Xiaofei on 2016/4/28.
 */
public class BgJobManager {

    private static JobManager mJobManager;

    private static JobManager mUploadManager;

    public static JobManager getManager() {
        return mJobManager;
    }

    public static JobManager getUploadManager() {
        return mUploadManager;
    }

    public static void init(Context context) {
        Configuration.Builder builder = new Configuration.Builder(context)
            .customLogger(new CustomLogger() {

                @Override
                public boolean isDebugEnabled() {
                    return true;
                }

                @Override
                public void d(String text, Object... args) {

                }

                @Override
                public void e(Throwable t, String text, Object... args) {

                }

                @Override
                public void e(String text, Object... args) {

                }

                @Override
                public void v(String text, Object... args) {

                }
            })
            .minConsumerCount(1)
            .maxConsumerCount(5)
            .loadFactor(1)
            .consumerKeepAlive(120);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.scheduler(FrameworkJobSchedulerService.createSchedulerFor(context, BgJobService.class), false);
        } else {
//            int enableGcm =

        }

        mJobManager = new JobManager(builder.build());
        mUploadManager = new JobManager(builder.build());
    }



}
