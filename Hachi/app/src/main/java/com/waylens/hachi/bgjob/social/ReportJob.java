package com.waylens.hachi.bgjob.social;

import android.support.annotation.Nullable;


import com.orhanobut.logger.Logger;
import com.waylens.hachi.jobqueue.Job;
import com.waylens.hachi.jobqueue.Params;
import com.waylens.hachi.jobqueue.RetryConstraint;
import com.waylens.hachi.rest.IHachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.ReportFeedbackBody;
import com.waylens.hachi.rest.body.ReportCommentBody;
import com.waylens.hachi.rest.body.ReportMomentBody;
import com.waylens.hachi.rest.body.ReportUserBody;
import com.waylens.hachi.rest.response.SimpleBoolResponse;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by Xiaofei on 2016/6/14.
 */
public class ReportJob extends Job {
    private static final String TAG = ReportJob.class.getSimpleName();
    public static final int REPORT_TYPE_MOMENT = 0;
    public static final int REPORT_TYPE_COMMENT = 1;
    public static final int REPORT_TYPE_USER = 2;
    public static final int REPORT_TYPE_FEEDBACK = 4;

    private int mtype;
    private Object mReportBody;


    public ReportJob(Object reportBody, int type) {
        super(new Params(0).requireNetwork());
        mReportBody = reportBody;
        mtype = type;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        Logger.t(TAG).d("do report");
        IHachiApi hachiApi = HachiService.createHachiApiService();
        Call<SimpleBoolResponse> response = null;

        switch (mtype) {
            case REPORT_TYPE_COMMENT:
                ReportCommentBody reportCommentBody = (ReportCommentBody) mReportBody;
                response = hachiApi.report(reportCommentBody);
                Logger.t(TAG).d("response: " + response.execute().body().result);
                break;
            case REPORT_TYPE_MOMENT:
                ReportMomentBody reportMomentBody = (ReportMomentBody) mReportBody;
                response = hachiApi.report(reportMomentBody);
                Logger.t(TAG).d("response: " + response.execute().body().result);
                break;
            case REPORT_TYPE_USER:
                ReportUserBody reportUserBody = (ReportUserBody) mReportBody;
                response = hachiApi.report(reportUserBody);
                Logger.t(TAG).d("response: " + response.execute().body().result);
                break;
            case REPORT_TYPE_FEEDBACK:
                ReportFeedbackBody reportFeedbackBody = (ReportFeedbackBody) mReportBody;
                response = hachiApi.report(reportFeedbackBody);
                Response<SimpleBoolResponse> simpleBoolResponse = response.execute();
                Logger.t(TAG).d("response: " + simpleBoolResponse.message());
                break;
            default:
                break;
        }

    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }
}
