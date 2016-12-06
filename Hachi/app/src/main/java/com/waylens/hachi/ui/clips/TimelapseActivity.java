package com.waylens.hachi.ui.clips;

/**
 * Created by lshw on 16/12/1.
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.export.ExportManager;
import com.waylens.hachi.bgjob.export.event.ExportEvent;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.clips.player.multisegseekbar.MultiSegSeekbar;
import com.waylens.hachi.ui.settings.adapters.DownloadItemAdapter;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/8/16.
 */
public class TimelapseActivity extends BaseActivity {

    public static final String TAG = TimelapseActivity.class.getSimpleName();
    private DownloadItemAdapter mDownloadItemAdapter;

    @BindView(R.id.download_list)
    RecyclerView mRvDownloadList;

    @BindView(R.id.root_switch)
    ViewSwitcher rootSwitch;

    @BindView(R.id.btn_export)
    Button mBtnExport;

    @BindView(R.id.radio_group)
    RadioGroup mTimelapseRadioGroup;

    @BindView(R.id.btnPlayPause)
    ImageButton mBtnPlayPause;

    @BindView(R.id.multiSegIndicator)
    MultiSegSeekbar mSeekbar;

    @BindView(R.id.duration)
    TextView mTvDuration;

    @BindView(R.id.btn_fullscreen)
    ImageButton mBtnFullScreen;




    public static void launch(Activity activity) {
        //install callback in bgjob
        ExportManager.getManager();
        Intent intent = new Intent(activity, TimelapseActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        EventBus.getDefault().register(TimelapseActivity.this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHandleExportJobEvent(ExportEvent event) {
        Logger.t(TAG).d("event type" + event.getWhat());
        if (mDownloadItemAdapter != null) {
            mDownloadItemAdapter.handleEvent(event);
        }
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(TimelapseActivity.this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    private void initViews() {
        setContentView(R.layout.activity_timelapse);
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
