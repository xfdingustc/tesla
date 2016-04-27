package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.AccountActivity;
import com.waylens.hachi.ui.activities.ManualSetupActivity;
import com.waylens.hachi.ui.activities.VersionCheckActivity;

import butterknife.OnClick;

/**
 * Created by Richard on 1/5/16.
 */
public class SettingsFragment extends BaseFragment {
    private static final String TAG = SettingsFragment.class.getSimpleName();
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

    @OnClick(R.id.addNewCamera)
    public void onAddNewCameraClicked() {
//        MaterialDialog materialDialog = new MaterialDialog.Builder(getActivity())
//            .items(R.array.setup_items)
//            .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
//                @Override
//                public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
//                    Logger.t(TAG).d("which: " + which + " text: " + text);
//                    switch (which) {
//                        case 0:
//                            SmartConfigActivity.launch(getActivity());
//                            break;
//                        case 1:
//                            MenualSetupActivity.launch(getActivity());
//                            break;
//                    }
//                    return true;
//                }
//            }).show();
        ManualSetupActivity.launch(getActivity());
    }
}
