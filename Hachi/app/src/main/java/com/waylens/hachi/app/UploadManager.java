package com.waylens.hachi.app;

import com.waylens.hachi.bgjob.upload.UploadMomentJob;
import com.waylens.hachi.bgjob.upload.event.UploadEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/7/13.
 */
public class UploadManager {
    private static UploadManager mSharedUploadManager = new UploadManager();
    private final List<UploadMomentJob> mUploadJobList;

    private EventBus mEventBus = EventBus.getDefault();

    private List<WeakReference<OnUploadJobStateChangeListener>> mListenerList;


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUploadEvent(UploadEvent event) {
        UploadMomentJob job = event.getJob();
        switch (event.getWhat()) {
            case UploadEvent.UPLOAD_JOB_STATE_CHANGED:
                notifyUploadStateChanged(job);
                break;
            case UploadEvent.UPLOAD_JOB_REMOVED:
                removeJob(job);
                break;
        }
    }

    private UploadManager() {
        this.mUploadJobList = new ArrayList<>();
        mListenerList = new ArrayList<>();
        mEventBus.register(this);
    }


    public static UploadManager getManager() {
        if (mSharedUploadManager == null) {
            mSharedUploadManager = new UploadManager();
        }
        return mSharedUploadManager;
    }

    public void addOnUploadJobStateChangedListener(OnUploadJobStateChangeListener listener) {
        mListenerList.add(new WeakReference<OnUploadJobStateChangeListener>(listener));
    }


    public void addJob(UploadMomentJob job) {
        mUploadJobList.add(job);
        for (int i = 0; i < mListenerList.size(); i++) {
            WeakReference<OnUploadJobStateChangeListener> oneListenr = mListenerList.get(i);
            if (oneListenr != null) {
                OnUploadJobStateChangeListener listener = oneListenr.get();

                if (listener != null) {
                    listener.onUploadJobAdded();
                }

            }


        }

    }

    private void removeJob(UploadMomentJob job) {
        mUploadJobList.remove(job);
        for (int i = 0; i < mListenerList.size(); i++) {
            WeakReference<OnUploadJobStateChangeListener> oneListenr = mListenerList.get(i);
            if (oneListenr != null) {
                OnUploadJobStateChangeListener listener = oneListenr.get();
                if (listener != null) {
                    listener.onUploadJobRemoved();
                }
            }
        }

    }

    public int getJobCount() {
        return mUploadJobList.size();
    }

    public int getUploadingJobCount() {
        int i = 0;
        for (UploadMomentJob job : mUploadJobList) {
            if (job.getState() != UploadMomentJob.UPLOAD_STATE_FINISHED) {
                i++;
            }
        }
        return i;
    }

    public UploadMomentJob getUploadJob(int index) {
        if (index >= mUploadJobList.size()) {
            return null;
        }

        return mUploadJobList.get(index);
    }


    public void notifyUploadStateChanged(UploadMomentJob job) {
        for (int i = 0; i < mUploadJobList.size(); i++) {
            UploadMomentJob oneJob = mUploadJobList.get(i);
            if (oneJob.getId() == job.getId()) {
                for (int j = 0; j < mListenerList.size(); j++) {
                    WeakReference<OnUploadJobStateChangeListener> oneListenr = mListenerList.get(j);
                    if (oneListenr != null) {
                        OnUploadJobStateChangeListener listener = oneListenr.get();
                        if (listener != null) {
                            listener.onUploadJobStateChanged(job, i);
                        }
                    }
                }
            }
        }
    }


    public interface OnUploadJobStateChangeListener {
        void onUploadJobStateChanged(UploadMomentJob job, int index);

        void onUploadJobAdded();

        void onUploadJobRemoved();
    }
}
