package com.waylens.hachi.ui.clips.upload;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.birbit.android.jobqueue.JobManager;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.upload.event.UploadEvent;
import com.waylens.hachi.ui.activities.BaseActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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
        mVideoItemAdapter = new VideoItemAdapter();
        mRvMyVideoList.setAdapter(mVideoItemAdapter);

        JobManager jobManager = BgJobManager.getManager();



    }
}
