package com.waylens.hachi.ui.clips.upload;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.birbit.android.jobqueue.JobManager;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.upload.event.UploadEvent;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.entities.Moment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.util.List;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/6/17.
 */
public class UploadActivity extends BaseActivity {
    private static final String TAG = UploadActivity.class.getSimpleName();

    private VideoItemAdapter mVideoItemAdapter;

    private EventBus mEventBus = EventBus.getDefault();

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, UploadActivity.class);
        activity.startActivity(intent);
    }


    @BindView(R.id.my_video_list)
    RecyclerView mRvMyVideoList;

    @Subscribe
    public void onEventUpload(UploadEvent event) {
        Logger.t(TAG).d("event: what: " + event.getWhat());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mEventBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mEventBus.unregister(this);
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_upload);
        setupToolbar();
        setupMyVideoList();
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.video);
    }

    private void setupMyVideoList() {
        mRvMyVideoList.setLayoutManager(new LinearLayoutManager(this));
        mVideoItemAdapter = new VideoItemAdapter(this);
        mRvMyVideoList.setAdapter(mVideoItemAdapter);
        mRvMyVideoList.setLayoutManager(new LinearLayoutManager(this));
        loadUserMoment(0, false);


    }



    private void loadUserMoment(int cursor, final boolean isRefresh) {
        SessionManager sessionManager = SessionManager.getInstance();


        final String requestUrl = Constants.API_USERS + "/" + sessionManager.getUserId() + "/moments?cursor=" + cursor;
//        Logger.t(TAG).d("requestUrl: " + requestUrl);
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(requestUrl)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
//                    Logger.t(TAG).json(response.toString());
                    List<Moment> momentList = Moment.parseMomentArray(response);
//                    mMomentRvAdapter.setMomentList(mMomentList);
//                    mCurrentCursor += mMomentList.size();
                    Logger.t(TAG).d("momentList: " + momentList.size());
                    mVideoItemAdapter.setUploadedMomentList(momentList);
//                    if (isRefresh) {
//                        mMomentRvAdapter.setMoments(mMomentList);
//                    } else {
//                        mMomentRvAdapter.addMoments(mMomentList);
//                    }
//
//                    mRvUserMomentList.setIsLoadingMore(false);
//                    if (!response.optBoolean("hasMore")) {
//                        mRvUserMomentList.setEnableLoadMore(false);
//                        mMomentRvAdapter.setHasMore(false);
//                    } else {
//                        mMomentRvAdapter.setHasMore(true);
//                    }
                }
            })
            .errorListener(new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }).build();


        mRequestQueue.add(request);
    }
}
