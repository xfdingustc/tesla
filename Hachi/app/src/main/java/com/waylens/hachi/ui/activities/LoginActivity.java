package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.fragments.LinkAccountFragment;
import com.waylens.hachi.ui.fragments.SignInFragment;
import com.waylens.hachi.ui.fragments.SignUpFragment;

/**
 * Created by Xiaofei on 2015/8/5.
 */
public class LoginActivity extends BaseActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();

    public static void launch(Activity startActivity) {
        Intent intent = new Intent(startActivity, LoginActivity.class);
        startActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_content, new SignInFragment())
                .commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_content);
        if (fragment instanceof SignInFragment) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_content);
        if (fragment != null && fragment instanceof SignUpFragment) {
            getFragmentManager().beginTransaction().remove(fragment).commit();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void setupToolbar() {
        if (mToolbar == null) {
            return;
        }
        mToolbar.setTitle(R.string.sign_up);
        mToolbar.setNavigationIcon(R.drawable.navbar_close);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
