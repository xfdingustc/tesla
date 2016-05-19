package com.waylens.hachi.ui.community;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.views.OnViewDragListener;

/**
 * Created by Xiaofei on 2016/5/19.
 */
public class MomentActivity extends BaseActivity {
    private static final String TAG = MomentActivity.class.getSimpleName();
    private Moment mMoment;

    public static void launch(Context activity, Moment moment) {
        Intent intent = new Intent(activity, MomentActivity.class);
        intent.putExtra("moment", moment);
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
        Intent intent = getIntent();
        mMoment = (Moment)intent.getSerializableExtra("moment");
        Logger.t(TAG).d("moment: " + mMoment.toString());
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_moment);
        MomentPlayFragment fragment = MomentPlayFragment.newInstance(mMoment, new OnViewDragListener() {
            @Override
            public void onStartDragging() {

            }

            @Override
            public void onStopDragging() {

            }
        });

        getFragmentManager().beginTransaction().replace(R.id.moment_play_container, fragment).commit();
    }
}
