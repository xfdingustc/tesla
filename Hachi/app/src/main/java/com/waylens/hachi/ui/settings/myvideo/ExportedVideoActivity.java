package com.waylens.hachi.ui.settings.myvideo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ViewSwitcher;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/8/16.
 */
public class ExportedVideoActivity extends BaseActivity {

    private DownloadItemAdapter mDownloadItemAdapter;

    @BindView(R.id.download_list)
    RecyclerView mRvDownloadList;

    @BindView(R.id.root_switch)
    ViewSwitcher rootSwitch;


    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, ExportedVideoActivity.class);
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
        setContentView(R.layout.activity_exported_video);
        setupToolbar();
        setupDownloadFileList();

        if (mDownloadItemAdapter.getItemCount() == 0) {
            rootSwitch.showNext();
        }
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.my_videos);

        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }


    private void setupDownloadFileList() {
        mRvDownloadList.setLayoutManager(new LinearLayoutManager(this));
        mDownloadItemAdapter = new DownloadItemAdapter(this);
        mRvDownloadList.setAdapter(mDownloadItemAdapter);
    }
}
