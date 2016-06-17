package com.waylens.hachi.bgjob.upload;

import android.content.Context;
import android.support.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;

import org.json.JSONObject;

/**
 * Created by Xiaofei on 2016/4/27.
 */
public class UploadAvatarJob extends Job {
    private static final String TAG = UploadAvatarJob.class.getSimpleName();
    private CloudInfo mCloudInfo;

    private final String file;

    public UploadAvatarJob(String file) {
        super(new Params(0).requireNetwork().setPersistent(false));
        this.file = file;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        Logger.t(TAG).d("on Run file: " + file);

        Context context = getApplicationContext();
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        RequestFuture<JSONObject > future = RequestFuture.newFuture();
        AuthorizedJsonRequest request = new AuthorizedJsonRequest(Constants.API_START_UPLOAD_AVATAR, future, future);

        requestQueue.add(request);
        JSONObject response = future.get();


        mCloudInfo = CloudInfo.parseFromJson(response);
        Logger.t(TAG).d("get CloudinfO: " + mCloudInfo.toString());
        DataUploader uploader = new DataUploader();
//        mCloudInfo = new CloudInfo("52.74.236.46", 35020, "qwertyuiopasdfgh");
        uploader.upload(mCloudInfo, file);

//        Logger.t(TAG).d("Start upload");
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }



    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return new RetryConstraint(false);
    }


}
