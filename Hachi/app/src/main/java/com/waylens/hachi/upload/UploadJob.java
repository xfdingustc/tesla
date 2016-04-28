package com.waylens.hachi.upload;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.utils.DataUploaderV2;

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
        DataUploaderV2 uploader = new DataUploaderV2();
//        mCloudInfo = new CloudInfo("52.74.236.46", 35020, "qwertyuiopasdfgh");
        uploader.upload(mCloudInfo, file, new DataUploaderV2.OnUploadListener() {
            @Override
            public void onUploadSuccessful() {
                Logger.t(TAG).d("upload listener");
            }

            @Override
            public void onUploadProgress(int percentage) {
                Logger.t(TAG).d("upload progress: " + percentage);
            }

            @Override
            public void onUploadError(int errorCode, int extraCode) {
                Logger.t(TAG).d("update error: ");
            }

            @Override
            public void onCancelUpload() {
                Logger.t(TAG).d("upload cancelled");
            }
        });
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
