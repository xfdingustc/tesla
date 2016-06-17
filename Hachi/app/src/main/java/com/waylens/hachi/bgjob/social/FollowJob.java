package com.waylens.hachi.bgjob.social;

import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.rest.HachiApi;
import com.rest.HachiService;
import com.rest.body.FollowPostBody;
import com.rest.body.LikePostBody;
import com.rest.response.LikeResponse;
import com.rest.response.SimpleBoolResponse;

import retrofit2.Call;

/**
 * Created by Xiaofei on 2016/6/12.
 */
public class FollowJob extends Job{
    private static final String TAG = FollowJob.class.getSimpleName();
    private final String mUserId;
    private final boolean mAddFollow;

    public FollowJob(String userId, boolean addFollow) {
        super(new Params(0).requireNetwork());
        this.mUserId = userId;
        this.mAddFollow = addFollow;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        HachiApi hachiApi = HachiService.createHachiApiService();
        Call<SimpleBoolResponse> responseCall;

        FollowPostBody body = new FollowPostBody();
        body.userID = mUserId;

        Logger.t(TAG).d("add follow: " + mAddFollow);

        if (mAddFollow) {
            responseCall = hachiApi.follow(body);
        } else {
            responseCall = hachiApi.unfollow(body);
        }

        Logger.t(TAG).d("response: " + responseCall.execute().body().result);
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }



    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }
}
