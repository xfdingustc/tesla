package com.waylens.hachi.ui.settings.myvideo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.upload.UploadManager;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;
import com.waylens.hachi.ui.activities.BaseActivity;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/9/12.
 */
public class UploadingMomentActivity extends BaseActivity {

    private UploadItemAdapter mUploadItemAdapter;

    private static final String EXTRA_AUTO_EXIT = "extra.auto.exit";

    private boolean mAutoExit;

    @BindView(R.id.uploading_list)
    RecyclerView mRvUploadingList;

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
        UploadManager.getManager().addOnUploadJobStateChangedListener(new UploadManager.OnUploadJobStateChangeListener() {
            @Override
            public void onUploadJobStateChanged(UploadMomentJob job, int index) {

            }

            @Override
            public void onUploadJobAdded() {

            }

            @Override
            public void onUploadJobRemoved() {
                if (mAutoExit && mUploadItemAdapter.getItemCount() == 0) {
                    finish();
                }
            }
        });
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_uploading);
        setupToolbar();
        mRvUploadingList.setLayoutManager(new LinearLayoutManager(this));
        mUploadItemAdapter = new UploadItemAdapter(this);
        mRvUploadingList.setAdapter(mUploadItemAdapter);
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.uploading);
    }
}
