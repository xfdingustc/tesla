package com.waylens.hachi.bgjob.social;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.rest.HachiApi;
import com.rest.HachiService;
import com.rest.body.LikePostBody;
import com.rest.body.ReportBody;
import com.rest.response.LikeResponse;
import com.rest.response.SimpleBoolResponse;

import retrofit2.Call;

/**
 * Created by Xiaofei on 2016/6/14.
 */
public class ReportJob extends Job {
    private static final String TAG = ReportJob.class.getSimpleName();
    private final String mReason;
    private long mMomentId;


    public ReportJob(long momentId, String reason) {
        super(new Params(0).requireNetwork());
        mMomentId = momentId;
        mReason = reason;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        Logger.t(TAG).d("do report");
        HachiApi hachiApi = HachiService.createHachiApiService();
        ReportBody reportBody = new ReportBody();
        if (mMomentId != -1) {
            reportBody.momentID = mMomentId;
        }

        reportBody.reason = mReason;
        Call<SimpleBoolResponse> response = hachiApi.report(reportBody);
        Logger.t(TAG).d("response: " + response.execute().body().result);
    }

    @Override
    protected void onCancel(int cancelReason) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }
}
