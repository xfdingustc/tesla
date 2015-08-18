package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.fragments.LinkAccountFragment;
import com.waylens.hachi.ui.fragments.SignInFragment;

/**
 * Created by Xiaofei on 2015/8/5.
 */
public class LoginActivity extends BaseActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_content, new SignInFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!TextUtils.isEmpty(SessionManager.getInstance().getToken())
                && SessionManager.getInstance().needLinkAccount()) {
            pushFragment(new LinkAccountFragment());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_content);
        if (fragment instanceof SignInFragment) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static void launch(Activity startActivity) {
        Intent intent = new Intent(startActivity, LoginActivity.class);
        startActivity.startActivity(intent);
    }

    public void pushFragment(Fragment fragment) {
        getFragmentManager().beginTransaction().replace(R.id.fragment_content, fragment).commit();
    }

}
