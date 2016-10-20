package com.waylens.hachi.ui.authorization;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.settings.ProfileSettingActivity;

import butterknife.BindView;
import butterknife.OnClick;


/**
 * Created by liushuwei on 2016/6/8.
 */
public class SignUpSucceedActivity extends BaseActivity {
    private static final String TAG = SignUpSucceedActivity.class.getSimpleName();

    @BindView(R.id.btn_complete_profile)
    Button mcompleteProfileButton;
    @BindView(R.id.sign_up_get_started)
    TextView mgetStarted;

    @OnClick(R.id.btn_complete_profile)
    public void onBtnCompleteProfileClicked() {
        ProfileSettingActivity.launch(this);
    }

    @OnClick(R.id.sign_up_get_started)
    public void onGetStartedClicked() {
        MainActivity.launch(this);
    }

    public static void launch(Context activity) {
        Intent intent = new Intent(activity, SignUpSucceedActivity.class);
        activity.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_succeed);
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(R.string.verify_email_address)
            .positiveText(R.string.ok)
            .negativeText(R.string.cancel)
            .show();
    }

}
