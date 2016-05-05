package com.waylens.hachi.bgjob.upload;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.ui.entities.LocalMoment;

/**
 * Created by Xiaofei on 2016/5/2.
 */
public class UploadMomentJob extends Job {
    private static final String TAG = UploadMomentJob.class.getSimpleName();
    private final LocalMoment mLocalMoment;

    public UploadMomentJob(LocalMoment moment) {
        super(new Params(0).requireNetwork().setPersistent(false));
        this.mLocalMoment = moment;
    }

    @Override
    public void onAdded() {
        Logger.t(TAG).d("on Added");
    }

    @Override
    public void onRun() throws Throwable {
        Logger.t(TAG).d("on Run");
        DataUploader uploader = new DataUploader();
//        mCloudInfo = new CloudInfo("52.74.236.46", 35020, "qwertyuiopasdfgh");
        uploader.upload(mLocalMoment);
    }

    @Override
    protected void onCancel(int cancelReason) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return new RetryConstraint(false);
    }
}
