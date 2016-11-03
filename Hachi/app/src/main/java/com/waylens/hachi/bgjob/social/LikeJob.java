package com.waylens.hachi.bgjob.social;

import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.jobqueue.Job;
import com.waylens.hachi.jobqueue.Params;
import com.waylens.hachi.jobqueue.RetryConstraint;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.LikePostBody;
import com.waylens.hachi.rest.response.LikeResponse;

import retrofit2.Call;


public class LikeJob extends Job {
    private static final String TAG = LikeJob.class.getSimpleName();
    private final long mMomentId;
    private final boolean mIsCancel;

    public LikeJob(long momentId, boolean isCancel) {
        super(new Params(0).requireNetwork());
        this.mMomentId = momentId;
        this.mIsCancel = isCancel;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        HachiApi hachiApi = HachiService.createHachiApiService();
        Call<LikeResponse> response = hachiApi.like(new LikePostBody(mMomentId, mIsCancel));
        Logger.t(TAG).d("response: " + response.execute().body().count);
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }


    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }
}
