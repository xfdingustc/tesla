package com.waylens.hachi.bgjob.social;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import com.orhanobut.logger.Logger;
import com.waylens.hachi.bgjob.social.event.SocialEvent;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.waylens.hachi.rest.IHachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.RepostBody;
import com.waylens.hachi.rest.response.RepostResponse;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Xiaofei on 2016/7/27.
 */
public class RepostJob extends Job {
    private static final String TAG = RepostJob.class.getSimpleName();
    private final long mMomentId;
    private final String mSocialProvider;

    private IHachiApi mHachi = HachiService.createHachiApiService();

    private EventBus mEventBus = EventBus.getDefault();

    public RepostJob(long momentId, String socialProvider) {
        super(new Params(0).requireNetwork());
        this.mMomentId = momentId;
        this.mSocialProvider = socialProvider;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        Logger.t(TAG).d("on run momentId: " + mMomentId + " provider: " + mSocialProvider);
        RepostBody body = new RepostBody(mMomentId, mSocialProvider);
        RepostResponse repostResponse = mHachi.repost(body).execute().body();
        mEventBus.post(new SocialEvent(SocialEvent.EVENT_WHAT_REPOST, repostResponse, mSocialProvider));
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }
}
