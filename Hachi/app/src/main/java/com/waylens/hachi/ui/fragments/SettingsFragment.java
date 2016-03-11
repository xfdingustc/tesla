package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.AccountActivity;
import com.waylens.hachi.ui.activities.VersionCheckActivity;

import butterknife.OnClick;

/**
 * Created by Richard on 1/5/16.
 */
public class SettingsFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_settings, savedInstanceState);
        return view;
    }

    @Override
    public void setupToolbar() {
        mToolbar.setTitle(R.string.settings);
        super.setupToolbar();
    }

    @OnClick(R.id.settings_account)
    void clickAccount() {
        AccountActivity.launch(getActivity());
    }

    @OnClick(R.id.settings_help)
    void clickHelp() {
        VersionCheckActivity.launch(getActivity());
    }
}
