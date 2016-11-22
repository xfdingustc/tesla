package com.waylens.hachi.ui.settings.myvideo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ViewAnimator;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.export.event.ExportEvent;
import com.waylens.hachi.bgjob.export.statejobqueue.CacheUploadMomentJob;
import com.waylens.hachi.bgjob.export.statejobqueue.CacheUploadMomentService;
import com.waylens.hachi.bgjob.export.statejobqueue.PersistentQueue;
import com.waylens.hachi.bgjob.export.statejobqueue.StateJobHolder;
import com.waylens.hachi.bgjob.upload.UploadManager;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;
import com.waylens.hachi.bgjob.upload.event.UploadMomentEvent;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.settings.adapters.UploadItemAdapter;
import com.waylens.hachi.ui.settings.adapters.UploadingItemAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/9/12.
 */
public class UploadingMomentActivity extends BaseActivity implements UploadManager.OnUploadJobStateChangeListener {
    private static final String TAG = UploadingMomentActivity.class.getSimpleName();
    private UploadItemAdapter mUploadItemAdapter;

    private UploadingItemAdapter mUploadingItemAdapter;

    private static final String EXTRA_AUTO_EXIT = "extra.auto.exit";

    private boolean mAutoExit;

    @BindView(R.id.uploading_list)
    RecyclerView mRvUploadingList;

    @BindView(R.id.root_switch)
    ViewAnimator rootAnimator;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHandleUploadJobEvent(UploadMomentEvent event) {
        Logger.t(TAG).d("event type" + event.getWhat());
        if (mUploadingItemAdapter != null) {
            mUploadingItemAdapter.handleEvent(event);
        }
    }

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, UploadingMomentActivity.class);
        activity.startActivity(intent);
    }

    public static void launch(Activity activity, boolean autoExit) {
        Intent intent = new Intent(activity, UploadingMomentActivity.class);
        intent.putExtra(EXTRA_AUTO_EXIT, autoExit);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        EventBus.getDefault().register(UploadingMomentActivity.this);
    }

    @Override
    protected void init() {
        super.init();
        mAutoExit = getIntent().getBooleanExtra(EXTRA_AUTO_EXIT, false);
        UploadManager.getManager().addOnUploadJobStateChangedListener(this);
        initViews();
    }

    @Override
    public void onUploadJobStateChanged(UploadMomentJob job, int index) {

    }

    @Override
    public void onUploadJobAdded() {
        rootAnimator.setDisplayedChild(0);
        Logger.t(TAG).d("onUploadJobAdded auto exit: " + mAutoExit + " count: " + mUploadItemAdapter.getItemCount());
    }

    @Override
    public void onUploadJobRemoved() {
        Logger.t(TAG).d("auto exit: " + mAutoExit + " count: " + mUploadItemAdapter.getItemCount());
        if (mUploadItemAdapter.getItemCount() == 0) {
            if (mAutoExit) {
                finish();
            } else {
                rootAnimator.setDisplayedChild(1);
            }
        }
    }

    private void initViews() {
        setContentView(R.layout.activity_uploading);
        setupToolbar();
        List<StateJobHolder> uploadJobList = PersistentQueue.getPersistentQueue().getAllJobs();
        Logger.t(TAG).d("uploadJobList size = " + uploadJobList.size());
        for (StateJobHolder jobHolder : uploadJobList) {
            Logger.t(TAG).d("insertId = " + jobHolder.getInsertId());
            Logger.t(TAG).d("jobId = " + jobHolder.getJobId());
            Logger.t(TAG).d("jobState = " + jobHolder.getJobState());
            //Logger.t(TAG).d("moment first segment url = " + ((CacheUploadMomentJob)jobHolder.getJob()).getLocalMoment().mSegments.get(0).uploadURL.url);
        }
        mRvUploadingList.setLayoutManager(new LinearLayoutManager(this));
/*        mUploadItemAdapter = new UploadItemAdapter(this);
        mRvUploadingList.setAdapter(mUploadItemAdapter);

        if (mUploadItemAdapter.getItemCount() == 0) {
            rootAnimator.setDisplayedChild(1);
        }*/
        mUploadingItemAdapter = new UploadingItemAdapter(this);
        mRvUploadingList.setAdapter(mUploadingItemAdapter);

        if (mUploadingItemAdapter.getItemCount() == 0) {
            rootAnimator.setDisplayedChild(1);
        } else {
            CacheUploadMomentService.launch(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.uploading);
    }
}
