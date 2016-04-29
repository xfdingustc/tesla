package com.waylens.hachi.upload;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;

/**
 * Created by Xiaofei on 2016/4/27.
 */
public class UploadJob extends Job {
    private static final String TAG = UploadJob.class.getSimpleName();
    private final CloudInfo mCloudInfo;

    private final String file;

    public UploadJob(String file, CloudInfo cloudInfo) {
        super(new Params(0).requireNetwork().persist());
        this.file = file;
        this.mCloudInfo = cloudInfo;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        Logger.t(TAG).d("on Run file: " + file);
        DataUploader uploader = new DataUploader();
//        mCloudInfo = new CloudInfo("52.74.236.46", 35020, "qwertyuiopasdfgh");
        uploader.upload(mCloudInfo, file);
//        Logger.t(TAG).d("Start upload");
    }

    @Override
    protected void onCancel(int cancelReason) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }


}
