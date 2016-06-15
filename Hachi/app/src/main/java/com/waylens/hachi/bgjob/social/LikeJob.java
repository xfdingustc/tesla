package com.waylens.hachi.bgjob.social;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.rest.HachiApi;
import com.rest.HachiService;
import com.rest.body.LikePostBody;
import com.rest.response.LikeResponse;

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
    protected void onCancel(int cancelReason) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }
}
