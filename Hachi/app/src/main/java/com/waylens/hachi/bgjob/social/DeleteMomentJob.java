package com.waylens.hachi.bgjob.social;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Response;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.rest.HachiApi;
import com.rest.HachiService;
import com.rest.response.SimpleBoolResponse;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.app.Hachi;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.http.DELETE;

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
//        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
//            .delete()
//            .url(Constants.API_MOMENTS + "/" + momentId)
//            .listner(new Response.Listener<JSONObject>() {
//                @Override
//                public void onResponse(JSONObject response) {
//
//                }
//            }).build();

        HachiApi hachiApi = HachiService.createHachiApiService();
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
