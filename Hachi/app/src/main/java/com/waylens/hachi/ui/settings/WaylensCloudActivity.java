package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.CloudStorageInfo;
import com.waylens.hachi.rest.response.MomentSummaryResponse;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.xfdingustc.rxutils.library.SimpleSubscribe;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/5/13.
 */
public class WaylensCloudActivity extends BaseActivity {
    private static final String TAG = WaylensCloudActivity.class.getSimpleName();

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, WaylensCloudActivity.class);
        activity.startActivity(intent);
    }

    @BindView(R.id.video_duration)
    TextView mVideoDuration;

    @BindView(R.id.video_size)
    TextView mVideoSize;

    @BindView(R.id.video_count)
    TextView mVideoCount;

    @BindView(R.id.weekly_quota)
    TextView mWeeklyQuota;

    @BindView(R.id.weekly_available)
    TextView mWeeklyAvailable;

    @BindView(R.id.cycle_info)
    TextView mCycleInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();


//        WaylensCloudFragment fragment = new WaylensCloudFragment();
//        getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }


    private void initViews() {
        setContentView(R.layout.activity_waylens_cloud);
        setupToolbar();
        getWaylensCloudInfo();
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.waylens_cloud);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    private void getWaylensCloudInfo() {

        HachiApi mHachi = HachiService.createHachiApiService();

        mHachi.getMomentSummaryRx()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<MomentSummaryResponse>() {
                @Override
                public void onNext(MomentSummaryResponse response) {
                    mVideoDuration.setText(String.valueOf(response.all.duration / 1000) + "s");
                    mVideoSize.setText(String.valueOf(response.all.size / (1024 * 1024)) + "MB");
                    mVideoCount.setText(String.valueOf(response.all.count));
                }
            });

        Call<CloudStorageInfo> createMomentResponseCall = mHachi.getCloudStorageInfo();
        createMomentResponseCall.enqueue(new Callback<CloudStorageInfo>() {
            @Override
            public void onResponse(Call<CloudStorageInfo> call, retrofit2.Response<CloudStorageInfo> response) {
                if (response.body() != null) {
                    CloudStorageInfo cloudStorageInfo = response.body();
                    int durationQuota = cloudStorageInfo.current.plan.durationQuota;
                    int durationUsed = cloudStorageInfo.current.durationUsed;
                    long cycleBegin = cloudStorageInfo.current.plan.cycleBegin;
                    long cycleEnd = cloudStorageInfo.current.plan.cycleEnd;
                    SimpleDateFormat format = new SimpleDateFormat("yyyy－MM－dd hh:mm");
                    mWeeklyQuota.setText(durationQuota / 60000 + "m");
                    mWeeklyAvailable.setText((durationQuota - durationUsed) / 60000 + "m");
                    mCycleInfo.setText("For the week " + format.format(new Date(cycleBegin)) + "~" + format.format(new Date(cycleEnd)) + ".");
                    Logger.t(TAG).d("used: " + cloudStorageInfo.current.durationUsed + "total: " + cloudStorageInfo.current.plan.durationQuota);
                }
            }

            @Override
            public void onFailure(Call<CloudStorageInfo> call, Throwable t) {

            }
        });
    }
}
