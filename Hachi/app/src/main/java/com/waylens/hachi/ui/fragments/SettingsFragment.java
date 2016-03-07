package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;

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
    public void onStart() {
        super.onStart();
        setTitle(R.string.settings);
    }

    @OnClick(R.id.settings_account)
    void clickAccount() {
        getFragmentManager().beginTransaction().replace(R.id.fragment_content, new AccountFragment()).commit();
    }

    @OnClick(R.id.settings_help)
    void clickHelp() {
        getFragmentManager().beginTransaction().replace(R.id.fragment_content, new VersionCheckFragment()).commit();
    }
}
