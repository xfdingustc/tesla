package com.waylens.hachi.bgjob.upload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.waylens.hachi.ui.entities.LocalMoment;

/**
 * Created by Xiaofei on 2016/9/6.
 */
public class UploadCachedMomentJob extends Job {

    private final LocalMoment mLocalMoment;

    public  UploadCachedMomentJob(LocalMoment moment) {
        super(new Params(0).requireNetwork().setPersistent(true));
        this.mLocalMoment = moment;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {

    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }
}
