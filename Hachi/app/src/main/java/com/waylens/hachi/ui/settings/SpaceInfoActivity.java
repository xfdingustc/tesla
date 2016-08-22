package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.utils.Utils;
import com.xfdingustc.snipe.SnipeError;
import com.xfdingustc.snipe.VdbResponse;
import com.xfdingustc.snipe.toolbox.GetSpaceInfoRequest;
import com.xfdingustc.snipe.vdb.SpaceInfo;

import butterknife.BindView;

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

        GetSpaceInfoRequest request = new GetSpaceInfoRequest(new VdbResponse.Listener<SpaceInfo>() {
            @Override
            public void onResponse(SpaceInfo response) {
                mSdCardVolume.setText(Utils.getSpaceString(response.total));
                mHightlight.setText(Utils.getSpaceString(response.marked));
                mVideoBuffer.setText(Utils.getSpaceString(response.used));
                mFree.setText(Utils.getSpaceString(response.total - response.used));
                mStorageProgressBar.setMax(100);
                long marked = (response.marked * 100) / response.total;
                long buffered = (response.used * 100) / response.total;
                mStorageProgressBar.setProgress((int)marked);
                mStorageProgressBar.setSecondaryProgress((int)buffered);
                mStorageNumber.setText(Utils.getSpaceNumber(response.used));
                mStorageUnit.setText(Utils.getSpaceUnit(response.used));
            }

        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });

        mVdtCamera.getRequestQueue().add(request);
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.external_storage);
    }
}
