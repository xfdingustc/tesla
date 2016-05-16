package com.waylens.hachi.bgjob.social;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.entities.Moment;

import org.json.JSONException;
import org.json.JSONObject;


public class LikeJob extends Job {
    private static final String TAG = LikeJob.class.getSimpleName();
    private final Moment mMoment;
    private final boolean mIsCancel;

    public LikeJob(Moment moment, boolean isCancel) {
        super(new Params(0).requireNetwork());
        this.mMoment = moment;
        this.mIsCancel = isCancel;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        Context context = getApplicationContext();
        RequestQueue requestQueue = Volley.newRequestQueue(context);

        JSONObject postBody = new JSONObject();
        try {
            postBody.put("momentID", mMoment.id);
            postBody.put("cancel", mIsCancel);
        } catch (JSONException e) {
            Logger.t(TAG).e("test", "", e);
        }

        RequestFuture<JSONObject> future = RequestFuture.newFuture();

        AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.POST,
            Constants.API_MOMENT_LIKE, postBody, future, future);
        requestQueue.add(request);

        JSONObject response = future.get();
    }

    @Override
    protected void onCancel(int cancelReason) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }
}
