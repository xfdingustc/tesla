package com.waylens.hachi.bgjob.upload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.bgjob.upload.event.UploadEvent;
import com.waylens.hachi.ui.entities.LocalMoment;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Xiaofei on 2016/9/6.
 */
public class UploadCachedMomentJob extends Job implements IUploadable {
    private static final String TAG = UploadCachedMomentJob.class.getSimpleName();

    private final LocalMoment mLocalMoment;

    private int mState = UPLOAD_STATE_WAITING_FOR_NETWORK_AVAILABLE;

    private int mProgress;

    private int mError;

    public UploadCachedMomentJob(LocalMoment moment) {
        super(new Params(0).requireNetwork().setPersistent(true));
        this.mLocalMoment = moment;
    }

    @Override
    public void onAdded() {
        Logger.t(TAG).d("on added");

    }

    @Override
    public void onRun() throws Throwable {
        Logger.t(TAG).d("on run " + mLocalMoment.toString());
        UploadManager.getManager().addJob(this);
        while (true) {
            Thread.sleep(10000);
        }
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }

    @Override
    public String getJobId() {
        return getId();
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public int getUploadProgress() {
        return mProgress;
    }

    @Override
    public int getUploadError() {
        return mError;
    }

    @Override
    public LocalMoment getLocalMoment() {
        return mLocalMoment;
    }

    @Override
    public void cancelUpload() {

    }
}
