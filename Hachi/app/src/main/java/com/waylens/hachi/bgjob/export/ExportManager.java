package com.waylens.hachi.bgjob.export;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.export.event.ExportEvent;
import com.waylens.hachi.jobqueue.Job;
import com.waylens.hachi.jobqueue.callback.JobManagerCallback;


import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/8/16.
 */
public class ExportManager {
    private static final String TAG = ExportManager.class.getSimpleName();
    private List<ExportableJob> mDownloadJobList;
//    private RxBus mRxBus = RxBus.getDefault();
    private EventBus mEventBus = EventBus.getDefault();

    private static ExportManager mSharedManager = null;

    public static ExportManager getManager() {
        if (mSharedManager == null) {
            synchronized (ExportManager.class) {
                if (mSharedManager == null) {
                    mSharedManager = new ExportManager();
                }
            }
        }
        return mSharedManager;
    }



    private ExportManager() {
        mDownloadJobList = new ArrayList<>();
        BgJobManager.getManager().addCallback(new JobManagerCallback() {
            @Override
            public void onJobAdded(@NonNull Job job) {
                if (job instanceof ExportableJob) {
                    ExportableJob exportableJob = (ExportableJob)job;
                    addJob(exportableJob);


                }
            }

            @Override
            public void onJobRun(@NonNull Job job, int resultCode) {

            }

            @Override
            public void onJobCancelled(@NonNull Job job, boolean byCancelRequest, @Nullable Throwable throwable) {
                if (job instanceof ExportableJob) {
                    removeJob((ExportableJob)job);
                }
            }

            @Override
            public void onDone(@NonNull Job job) {
                Logger.t(TAG).d("on job done");
                if (job instanceof  ExportableJob) {
                    removeJob((ExportableJob)job);
                }

            }

            @Override
            public void onAfterJobRun(@NonNull Job job, int resultCode) {
                Logger.t(TAG).d("onAfterJobRun");
            }
        });
    }



    public void addJob(ExportableJob job) {
        mDownloadJobList.add(job);
        Logger.t(TAG).d("add job event");
        mEventBus.post(new ExportEvent(ExportEvent.EXPORT_WHAT_JOB_ADDED, job));
        job.setOnProgressChangedListener(new ExportableJob.OnProgressChangedListener() {
            @Override
            public void OnProgressChanged(ExportableJob job) {
                Logger.t(TAG).d("export job progress event");
                for (int i = 0; i < mDownloadJobList.size(); i++) {
                    ExportableJob oneJob = mDownloadJobList.get(i);
                    if (oneJob.getId().equals(job.getId())) {
                        Logger.t(TAG).d("export job event post");
                        mEventBus.post(new ExportEvent(ExportEvent.EXPORT_WHAT_PROGRESS, job, i));
                    }
                }
            }
        });
    }

    public void removeJob(ExportableJob job) {
        mDownloadJobList.remove(job);
        Logger.t(TAG).d("remove job");
        mEventBus.post(new ExportEvent(ExportEvent.EXPORT_WHAT_FINISHED, job));
    }

    public int getCount() {
        return mDownloadJobList.size();
    }

    public ExportableJob getDownloadJob(int index) {
        if (index >= 0 && index < mDownloadJobList.size()) {
            return mDownloadJobList.get(index);
        } else {
            return null;
        }
    }


}
