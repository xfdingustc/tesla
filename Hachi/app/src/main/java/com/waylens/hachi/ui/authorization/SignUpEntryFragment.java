package com.waylens.hachi.ui.authorization;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;

import butterknife.OnClick;

/**
 * Created by Richard on 3/11/16.
 */
public class SignUpEntryFragment extends BaseFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_signup_entry, savedInstanceState);
    }

    @OnClick(R.id.btn_sign_up)
    void clickSignUp() {
        AuthorizeActivity.launchForResult(getActivity(), MainActivity.REQUEST_CODE_SIGN_UP_FROM_MOMENTS);
    }
}

