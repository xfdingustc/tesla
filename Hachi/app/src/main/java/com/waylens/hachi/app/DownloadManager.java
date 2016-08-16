package com.waylens.hachi.app;

import com.waylens.hachi.bgjob.download.DownloadJob;
import com.waylens.hachi.bgjob.download.event.DownloadEvent;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;

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
    private List<DownloadJob> mDownloadJobList;
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
        mEventBus.register(this);
    }

    public void addListener(OnDownloadJobStateChangeListener listener) {
        mListenerList.add(new WeakReference<>(listener));
    }

    public void addJob(DownloadJob job) {
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

    public void removeJob(DownloadJob job) {
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

    public DownloadJob getDownloadJob(int index) {
        return mDownloadJobList.get(index);
    }

    public void notifyUploadStateChanged(DownloadJob job) {
        for (int i = 0; i < mDownloadJobList.size(); i++) {
            DownloadJob oneJob = mDownloadJobList.get(i);
            if (oneJob.getId() == job.getId()) {
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
        void onDownloadJobStateChanged(DownloadJob job, int index);

        void onDownloadJobAdded();

        void onDownloadJobRemoved();
    }
}
