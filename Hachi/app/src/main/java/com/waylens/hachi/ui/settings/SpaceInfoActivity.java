package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.reative.SnipeApiRx;
import com.waylens.hachi.snipe.toolbox.GetSpaceInfoRequest;
import com.waylens.hachi.snipe.vdb.SpaceInfo;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.utils.Utils;
import com.xfdingustc.rxutils.library.SimpleSubscribe;


import butterknife.BindView;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/8/22.
 */
public class SpaceInfoActivity extends BaseActivity {

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, SpaceInfoActivity.class);
        activity.startActivity(intent);
    }

    @BindView(R.id.storage_progress_bar)
    ProgressBar mStorageProgressBar;

    @BindView(R.id.tv_storage_number)
    TextView mStorageNumber;

    @BindView(R.id.tv_storage_unit)
    TextView mStorageUnit;

    @BindView(R.id.tv_sd_card_volume)
    TextView mSdCardVolume;

    @BindView(R.id.tv_highlights)
    TextView mHightlight;

    @BindView(R.id.tv_video_buffer)
    TextView mVideoBuffer;

    @BindView(R.id.tv_free)
    TextView mFree;

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
        setContentView(R.layout.activity_space_info);
        setupToolbar();

        SnipeApiRx.getSpaceInfoRx()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<SpaceInfo>() {
                @Override
                public void onNext(SpaceInfo spaceInfo) {
                    mSdCardVolume.setText(Utils.getSpaceString(spaceInfo.total));
                    mHightlight.setText(Utils.getSpaceString(spaceInfo.marked));
                    mVideoBuffer.setText(Utils.getSpaceString(spaceInfo.used));
                    mFree.setText(Utils.getSpaceString(spaceInfo.total - spaceInfo.used));
                    mStorageProgressBar.setMax(100);
                    long marked = (spaceInfo.marked * 100) / spaceInfo.total;
                    long buffered = (spaceInfo.used * 100) / spaceInfo.total;
                    mStorageProgressBar.setProgress((int)marked);
                    mStorageProgressBar.setSecondaryProgress((int)buffered);
                    mStorageNumber.setText(Utils.getSpaceNumber(spaceInfo.used));
                    mStorageUnit.setText(Utils.getSpaceUnit(spaceInfo.used));
                }
            });


    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.external_storage);
    }
}
