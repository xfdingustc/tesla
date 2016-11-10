package com.waylens.hachi.bgjob.upload;

import com.orhanobut.logger.Logger;
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
    private static final String TAG = UploadManager.class.getSimpleName();
    private static UploadManager mSharedUploadManager = new UploadManager();
    private final List<UploadMomentJob> mUploadables;

    private EventBus mEventBus = EventBus.getDefault();

    private List<WeakReference<OnUploadJobStateChangeListener>> mListenerList;


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUploadEvent(UploadEvent event) {
        UploadMomentJob uploadable = event.getUploadable();
        switch (event.getWhat()) {
            case UploadEvent.UPLOAD_JOB_ADDED:
                addJob(uploadable);
                break;
            case UploadEvent.UPLOAD_JOB_STATE_CHANGED:
                notifyUploadStateChanged(uploadable);
                break;
            case UploadEvent.UPLOAD_JOB_REMOVED:
                removeJob(uploadable);
                break;
        }
    }

    private UploadManager() {
        this.mUploadables = new ArrayList<>();
        mListenerList = new ArrayList<>();
        mEventBus.register(this);
    }


    public static UploadManager getManager() {
        return mSharedUploadManager;
    }

    public void addOnUploadJobStateChangedListener(OnUploadJobStateChangeListener listener) {
        mListenerList.add(new WeakReference<>(listener));
    }


    public void addJob(UploadMomentJob job) {
        for (int i = 0; i < mUploadables.size(); i++) {
            UploadMomentJob oneJob = mUploadables.get(i);
            if (oneJob.getJobId().equals(job.getJobId())) {
                return;
            }
        }

        mUploadables.add(job);
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
        Logger.t(TAG).d("before job size: " + mUploadables.size());
        mUploadables.remove(job);
        Logger.t(TAG).d("after job size: " + mUploadables.size());
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
        return mUploadables.size();
    }

    public int getUploadingJobCount() {
        int i = 0;
        for (UploadMomentJob job : mUploadables) {
            if (job.getState() != UploadMomentJob.UPLOAD_STATE_FINISHED) {
                i++;
            }
        }
        return i;
    }

    public UploadMomentJob getUploadJob(int index) {
        if (index >= mUploadables.size()) {
            return null;
        }

        return mUploadables.get(index);
    }


    public void notifyUploadStateChanged(UploadMomentJob job) {
        for (int i = 0; i < mUploadables.size(); i++) {
            UploadMomentJob oneJob = mUploadables.get(i);
            if (oneJob.getJobId().equals(job.getJobId())) {
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
