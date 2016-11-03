package com.waylens.hachi.app;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.download.ExportableJob;
import com.waylens.hachi.bgjob.download.event.DownloadEvent;
import com.waylens.hachi.jobqueue.Job;
import com.waylens.hachi.jobqueue.callback.JobManagerCallback;
import com.waylens.hachi.utils.rxjava.RxBus;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/8/16.
 */
public class DownloadManager {
    private static final String TAG = DownloadManager.class.getSimpleName();
    private List<ExportableJob> mDownloadJobList;
    private RxBus mRxBus = RxBus.getDefault();

    private static DownloadManager mSharedManager = null;

    public static DownloadManager getManager() {
        if (mSharedManager == null) {
            synchronized (DownloadManager.class) {
                if (mSharedManager == null) {
                    mSharedManager = new DownloadManager();
                }
            }
        }

        return mSharedManager;
    }



    private DownloadManager() {
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
        mRxBus.post(new DownloadEvent(DownloadEvent.DOWNLOAD_WHAT_JOB_ADDED, job));
        job.setOnProgressChangedListener(new ExportableJob.OnProgressChangedListener() {
            @Override
            public void OnProgressChanged(ExportableJob job) {
                for (int i = 0; i < mDownloadJobList.size(); i++) {
                    ExportableJob oneJob = mDownloadJobList.get(i);
                    if (oneJob.getId().equals(job.getId())) {
                        
                        mRxBus.post(new DownloadEvent(DownloadEvent.DOWNLOAD_WHAT_PROGRESS, job, i));
                    }
                }
            }
        });
    }

    public void removeJob(ExportableJob job) {
        mDownloadJobList.remove(job);
        Logger.t(TAG).d("remove job");
        mRxBus.post(new DownloadEvent(DownloadEvent.DOWNLOAD_WHAT_FINISHED, job));
    }

    public int getCount() {
        return mDownloadJobList.size();
    }

    public ExportableJob getDownloadJob(int index) {
        return mDownloadJobList.get(index);
    }


}
