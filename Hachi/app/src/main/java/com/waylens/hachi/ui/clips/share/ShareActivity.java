package com.waylens.hachi.ui.clips.share;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class ShareActivity extends BaseActivity {

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, ShareActivity.class);
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
        setContentView(R.layout.activity_share);
    }
}
