package com.waylens.hachi.ui.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.manualsetup.StartupActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Richard on 1/5/16.
 */
public class SettingsFragment extends BaseFragment {
    private static final String TAG = SettingsFragment.class.getSimpleName();

    @BindView(R.id.camera_setting)
    View mCameraSetting;

    @OnClick(R.id.camera_setting)
    public void onCameraSettingClicked() {
        if (VdtCameraManager.getManager().isConnected()) {
            CameraSettingActivity.launch(getActivity());
        }
    }

    @OnClick(R.id.waylens_cloud)
    public void onWaylensCloudClick() {
        WaylensCloudActivity.launch(getActivity());
    }

    @OnClick(R.id.settings_account)
    public void clickAccount() {
        if (SessionManager.getInstance().isLoggedIn()) {
            AccountActivity.launch(getActivity());
        } else {
            AuthorizeActivity.launch(getActivity());
        }
    }

    @OnClick(R.id.settings_help)
    public void clickHelp() {
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
        //ManualSetupActivity.launch(getActivity());
        StartupActivity.launch(getActivity());
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCameraConnectChangeEvent(CameraConnectionEvent event) {
        initViews();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_settings, savedInstanceState);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        initViews();
    }

    @Override
    protected String getRequestTag() {
        return TAG;
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    private void initViews() {
        if (!VdtCameraManager.getManager().isConnected()) {
            mCameraSetting.setVisibility(View.GONE);
        } else {
            mCameraSetting.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setupToolbar() {
        getToolbar().setTitle(R.string.settings);
        super.setupToolbar();
    }

//    @OnClick(R.id.general)
//    public void onGeneralClicked() {
//        GeneralSettingActivity.launch(getActivity());
//    }


}
