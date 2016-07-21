package com.waylens.hachi.bgjob.social;

/**
 * Created by lshw on 16/7/20.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.DeleteCommentResponse;

import retrofit2.Call;

/**
 * Created by Xiaofei on 2016/6/17.
 */
public class DeleteCommentJob extends Job {
    private static final String TAG = DeleteCommentJob.class.getSimpleName();
    private final long mCommentId;

    public DeleteCommentJob(long commentId) {
        super(new Params(0).requireNetwork());
        this.mCommentId = commentId;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        HachiApi hachiApi = HachiService.createHachiApiService();
        Call<DeleteCommentResponse> boolResponseCall = hachiApi.deleteComment(mCommentId);

        Logger.t(TAG).d("result: " + boolResponseCall.execute().body().count);
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }
}
