package com.waylens.hachi.bgjob.upload;

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
    private final List<IUploadable> mUploadables;

    private EventBus mEventBus = EventBus.getDefault();

    private List<WeakReference<OnUploadJobStateChangeListener>> mListenerList;


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUploadEvent(UploadEvent event) {
        IUploadable uploadable = event.getUploadable();
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
        mListenerList.add(new WeakReference<OnUploadJobStateChangeListener>(listener));
    }


    public void addJob(IUploadable job) {
        for (int i = 0; i < mUploadables.size(); i++) {
            IUploadable oneJob = mUploadables.get(i);
            if (oneJob.getJobId() == job.getJobId()) {
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

    private void removeJob(IUploadable job) {
        mUploadables.remove(job);
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
        for (IUploadable job : mUploadables) {
            if (job.getState() != IUploadable.UPLOAD_STATE_FINISHED) {
                i++;
            }
        }
        return i;
    }

    public IUploadable getUploadJob(int index) {
        if (index >= mUploadables.size()) {
            return null;
        }

        return mUploadables.get(index);
    }


    public void notifyUploadStateChanged(IUploadable job) {
        for (int i = 0; i < mUploadables.size(); i++) {
            IUploadable oneJob = mUploadables.get(i);
            if (oneJob.getJobId() == job.getJobId()) {
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
        void onUploadJobStateChanged(IUploadable job, int index);

        void onUploadJobAdded();

        void onUploadJobRemoved();
    }
}
