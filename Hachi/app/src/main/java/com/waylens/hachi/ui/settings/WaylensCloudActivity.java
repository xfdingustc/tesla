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
import com.waylens.hachi.ui.activities.BaseActivity;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/5/13.
 */
public class WaylensCloudActivity extends BaseActivity {
    private static final String TAG = WaylensCloudActivity.class.getSimpleName();

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, WaylensCloudActivity.class);
        activity.startActivity(intent);
    }

    @BindView(R.id.hd_duration)
    TextView mHdDuration;

    @BindView(R.id.hd_size)
    TextView mHdSize;

    @BindView(R.id.hd_count)
    TextView mHdCount;

    @BindView(R.id.sd_duration)
    TextView mSdDuration;

    @BindView(R.id.sd_size)
    TextView mSdSize;

    @BindView(R.id.sd_count)
    TextView mSdCount;

    @BindView(R.id.all_duration)
    TextView mAllDuration;

    @BindView(R.id.all_size)
    TextView mAllSize;

    @BindView(R.id.all_count)
    TextView mAllCount;

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
        String url = Constants.API_MOMENTS_SUMMARY;
        AuthorizedJsonRequest request = new AuthorizedJsonRequest(url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Logger.t(TAG).json(response.toString());

                try {
                    JSONObject hd = response.getJSONObject("hd");
                    mHdDuration.setText(String.valueOf(hd.getLong("duration") / 1000) + "ms");
                    mHdSize.setText(String.valueOf(hd.getLong("size") / (1024 * 1024)) + "M");
                    mHdCount.setText(String.valueOf(hd.getInt("count")));

                    JSONObject sd = response.getJSONObject("sd");
                    mSdDuration.setText(String.valueOf(sd.getLong("duration") / 1000) + "ms");
                    mSdSize.setText(String.valueOf(sd.getLong("size") / (1024 * 1024)) + "M");
                    mSdCount.setText(String.valueOf(sd.getInt("count")));

                    JSONObject all = response.getJSONObject("all");
                    mAllDuration.setText(String.valueOf(all.getLong("duration") / 1000) + "ms");
                    mAllSize.setText(String.valueOf(all.getLong("size") / (1024 * 1024)) + "M");
                    mAllCount.setText(String.valueOf(all.getInt("count")));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        mRequestQueue.add(request);
    }
}
