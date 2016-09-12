package com.waylens.hachi.ui.settings.myvideo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/9/12.
 */
public class UploadingMomentActivity extends BaseActivity {

    private UploadItemAdapter mUploadItemAdapter;

    @BindView(R.id.uploading_list)
    RecyclerView mRvUploadingList;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, UploadingMomentActivity.class);
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
