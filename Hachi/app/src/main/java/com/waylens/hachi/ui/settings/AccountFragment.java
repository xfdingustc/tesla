package com.waylens.hachi.ui.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.waylens.hachi.R;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.camera.events.CameraConnectionEvent;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.NotificationActivity;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.manualsetup.StartupActivity;
import com.waylens.hachi.ui.settings.myvideo.DownloadVideoActivity;
import com.waylens.hachi.ui.settings.myvideo.MyMomentActivity;
import com.waylens.hachi.ui.settings.myvideo.UploadingMomentActivity;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2016/9/28.
 */

public class AccountFragment extends BaseFragment {

    @BindView(R.id.user_avatar)
    CircleImageView userAvatar;

    @BindView(R.id.ll_waylens_cloud)
    View llWaylensCloud;

    @BindView(R.id.ll_camera_setting)
    View llCameraSetting;

    @BindView(R.id.user_name)
    TextView userName;

    @OnClick(R.id.ll_user_profile)
    public void onUserProfileClicked() {
        if (SessionManager.getInstance().isLoggedIn()) {
            ProfileSettingActivity.launch(getActivity());
        } else {
            AuthorizeActivity.launch(getActivity());
        }
    }

    @OnClick(R.id.ll_waylens_cloud)
    public void onWaylensCloudClicked() {
        WaylensCloudActivity.launch(getActivity());
    }

    @OnClick(R.id.ll_exported_video)
    public void onExportedVideoClicked() {
        DownloadVideoActivity.launch(getActivity());
    }

    @OnClick(R.id.ll_uploading)
    public void onUploadClicked() {
        UploadingMomentActivity.launch(getActivity());
    }

    @OnClick(R.id.ll_my_moment)
    public void onMyMomentClicked() {
        MyMomentActivity.launch(getActivity());
    }

    @OnClick(R.id.ll_notificatino)
    public void onNotificatinoClicked() {
        NotificationActivity.launch(getActivity());
    }

    @OnClick(R.id.ll_camera_setting)
    public void onCameraSettingClicked() {
        CameraSettingActivity.launch(getActivity());
    }

    @OnClick(R.id.ll_setup_camera)
    public void onCameraSetupClicked() {
        StartupActivity.launch(getActivity());
    }

    @OnClick(R.id.ll_setting)
    public void onSettingClicked() {
        SettingActivity.launch(getActivity());
    }


    @OnClick(R.id.ll_about)
    public void onAboutClicked() {
        AboutActivity.launch(getActivity());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCameraConnectChangeEvent(CameraConnectionEvent event) {
        initViews();
    }

    @Override
    protected String getRequestTag() {
        return AccountFragment.class.getSimpleName();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_account, savedInstanceState);
        initViews();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        initViews();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    private void initViews() {
        setupToolbar();
        if (SessionManager.getInstance().isLoggedIn()) {
            Glide.with(this)
                .load(SessionManager.getInstance().getAvatarUrl())
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(userAvatar);
            userName.setText(SessionManager.getInstance().getUserName());
            llWaylensCloud.setVisibility(View.VISIBLE);
        } else {
            userName.setText(R.string.click_2_login);
            llWaylensCloud.setVisibility(View.GONE);
        }

        if (VdtCameraManager.getManager().isConnected()) {
            llCameraSetting.setVisibility(View.VISIBLE);
        } else {
            llCameraSetting.setVisibility(View.GONE);
        }

    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.account);
    }
}