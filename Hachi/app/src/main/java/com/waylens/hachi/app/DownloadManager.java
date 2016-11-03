package com.waylens.hachi.app;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.download.Exportable;
import com.waylens.hachi.bgjob.download.event.DownloadEvent;
import com.waylens.hachi.jobqueue.Job;
import com.waylens.hachi.jobqueue.JobManager;
import com.waylens.hachi.jobqueue.callback.JobManagerCallback;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/8/16.
 */
public class DownloadManager {
    private EventBus mEventBus = EventBus.getDefault();
    private List<Exportable> mDownloadJobList;
    private List<WeakReference<OnDownloadJobStateChangeListener>> mListenerList;

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadEvent(DownloadEvent event) {
        switch (event.getWhat()) {
            case DownloadEvent.DOWNLOAD_WHAT_PROGRESS:
                notifyUploadStateChanged(event.getJob());
                break;
            case DownloadEvent.DOWNLOAD_WHAT_FINISHED:
                removeJob(event.getJob());
                break;
        }
    }

    private DownloadManager() {
        mDownloadJobList = new ArrayList<>();
        mListenerList = new ArrayList<>();
        BgJobManager.getManager().addCallback(new JobManagerCallback() {
            @Override
            public void onJobAdded(@NonNull Job job) {
                if (job instanceof Exportable) {
                    addJob((Exportable)job);
                }
            }

            @Override
            public void onJobRun(@NonNull Job job, int resultCode) {

            }

            @Override
            public void onJobCancelled(@NonNull Job job, boolean byCancelRequest, @Nullable Throwable throwable) {
                if (job instanceof Exportable) {
                    removeJob((Exportable)job);
                }
            }

            @Override
            public void onDone(@NonNull Job job) {
                if (job instanceof Exportable) {
                    removeJob((Exportable)job);
                }
            }

            @Override
            public void onAfterJobRun(@NonNull Job job, int resultCode) {

            }
        });
        mEventBus.register(this);
    }

    public void addListener(OnDownloadJobStateChangeListener listener) {
        mListenerList.add(new WeakReference<>(listener));
    }

    public void addJob(Exportable job) {
        mDownloadJobList.add(job);
        for (int i = 0; i < mListenerList.size(); i++) {
            WeakReference<OnDownloadJobStateChangeListener> oneListenr = mListenerList.get(i);
            if (oneListenr != null) {
                OnDownloadJobStateChangeListener listener = oneListenr.get();
                if (listener != null) {
                    listener.onDownloadJobAdded();
                }
            }
        }
    }

    public void removeJob(Exportable job) {
        mDownloadJobList.remove(job);
        for (int i = 0; i < mListenerList.size(); i++) {
            WeakReference<OnDownloadJobStateChangeListener> oneListenr = mListenerList.get(i);
            if (oneListenr != null) {
                OnDownloadJobStateChangeListener listener = oneListenr.get();
                if (listener != null) {
                    listener.onDownloadJobRemoved();
                }
            }
        }
    }

    public int getCount() {
        return mDownloadJobList.size();
    }

    public Exportable getDownloadJob(int index) {
        return mDownloadJobList.get(index);
    }

    public void notifyUploadStateChanged(Exportable job) {
        for (int i = 0; i < mDownloadJobList.size(); i++) {
            Exportable oneJob = mDownloadJobList.get(i);
            if (oneJob.getJobId() == job.getJobId()) {
                for (int j = 0; j < mListenerList.size(); j++) {
                    WeakReference<OnDownloadJobStateChangeListener> oneListenr = mListenerList.get(j);
                    if (oneListenr != null) {
                        OnDownloadJobStateChangeListener listener = oneListenr.get();
                        if (listener != null) {
                            listener.onDownloadJobStateChanged(job, i);
                        }
                    }
                }
            }
        }
    }

    public interface OnDownloadJobStateChangeListener {
        void onDownloadJobStateChanged(Exportable job, int index);

        void onDownloadJobAdded();

        void onDownloadJobRemoved();
    }
}
