package com.waylens.hachi.bgjob.social;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import com.orhanobut.logger.Logger;
import com.waylens.hachi.jobqueue.Job;
import com.waylens.hachi.jobqueue.Params;
import com.waylens.hachi.jobqueue.RetryConstraint;
import com.waylens.hachi.rest.IHachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.SimpleBoolResponse;

import retrofit2.Call;

/**
 * Created by Xiaofei on 2016/6/17.
 */
public class DeleteMomentJob extends Job {
    private static final String TAG = DeleteMomentJob.class.getSimpleName();
    private final long mMomentId;

    public DeleteMomentJob(long momentId) {
        super(new Params(0).requireNetwork());
        this.mMomentId = momentId;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        IHachiApi hachiApi = HachiService.createHachiApiService();
        Call<SimpleBoolResponse> boolResponseCall = hachiApi.deleteMoment(mMomentId);

        Logger.t(TAG).d("result: " + boolResponseCall.execute().body().result);
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }
}
