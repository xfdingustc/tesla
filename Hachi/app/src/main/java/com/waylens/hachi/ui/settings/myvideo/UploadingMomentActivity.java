package com.waylens.hachi.ui.settings.myvideo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ViewSwitcher;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.upload.UploadManager;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;
import com.waylens.hachi.ui.activities.BaseActivity;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/9/12.
 */
public class UploadingMomentActivity extends BaseActivity implements UploadManager.OnUploadJobStateChangeListener {
    private static final String TAG = UploadingMomentActivity.class.getSimpleName();
    private UploadItemAdapter mUploadItemAdapter;

    private static final String EXTRA_AUTO_EXIT = "extra.auto.exit";

    private boolean mAutoExit;

    @BindView(R.id.uploading_list)
    RecyclerView mRvUploadingList;

    @BindView(R.id.root_switch)
    ViewSwitcher rootSwitch;

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
        rootSwitch.showPrevious();
    }

    @Override
    public void onUploadJobRemoved() {
        Logger.t(TAG).d("auto exit: " + mAutoExit + " count: " + mUploadItemAdapter);
        if (mUploadItemAdapter.getItemCount() == 0) {
            if (mAutoExit) {
                finish();
            } else {
                rootSwitch.showNext();
            }
        }
    }

    private void initViews() {
        setContentView(R.layout.activity_uploading);
        setupToolbar();
        mRvUploadingList.setLayoutManager(new LinearLayoutManager(this));
        mUploadItemAdapter = new UploadItemAdapter(this);
        mRvUploadingList.setAdapter(mUploadItemAdapter);

        if (mUploadItemAdapter.getItemCount() == 0) {
            rootSwitch.showNext();
        }
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.uploading);
    }
}
