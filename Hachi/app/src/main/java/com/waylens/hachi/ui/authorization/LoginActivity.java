package com.waylens.hachi.ui.authorization;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.transitions.MorphTransform;

import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/10/31.
 */

public class LoginActivity extends BaseActivity {

    public static void launch(Activity activity, View transitionView) {
        Intent intent = new Intent(activity, LoginActivity.class);
        MorphTransform.addExtras(intent,
            ContextCompat.getColor(activity, R.color.hachi),
            activity.getResources().getDimensionPixelSize(R.dimen.dialog_corners));
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation
            (activity, transitionView, activity.getString(R.string.transition_hachi_login));
        activity.startActivity(intent, options.toBundle());
    }

    @OnClick(R.id.root_view)
    public void onRootViewClicked() {
        dismiss();
    }

    @OnClick(R.id.login)
    public void onLoginClicked() {
        AuthorizeActivity.launch(this);
    }

    private void dismiss() {
        finishAfterTransition();
    }


    @Override
    public void onBackPressed() {
        dismiss();
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
        setContentView(R.layout.activity_login);
    }
}
