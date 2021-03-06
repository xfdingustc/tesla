package com.waylens.hachi.ui.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.camera.events.CameraConnectionEvent;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.NotificationActivity;
import com.waylens.hachi.ui.activities.WebViewActivity;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.manualsetup.StartupActivity;
import com.waylens.hachi.ui.settings.adapters.SimpleExportedItemAdapter;
import com.waylens.hachi.ui.settings.myvideo.ExportedVideoActivity;
import com.waylens.hachi.ui.settings.myvideo.MyMomentActivity;
import com.waylens.hachi.ui.settings.myvideo.UploadingMomentActivity;
import com.waylens.hachi.ui.views.AvatarView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/9/28.
 */

public class AccountFragment extends BaseFragment implements FragmentNavigator {

    private SimpleExportedItemAdapter mExportedAdapter;

    @BindView(R.id.user_avatar)
    AvatarView userAvatar;

    @BindView(R.id.ll_waylens_cloud)
    View llWaylensCloud;

    @BindView(R.id.ll_camera_setting)
    View llCameraSetting;

    @BindView(R.id.ll_uploading)
    View llUploading;

    @BindView(R.id.exported_videos)
    RecyclerView rvExportedVideos;

    @BindView(R.id.ll_my_moment)
    View llMyMoment;

    @BindView(R.id.tvNotification)
    ImageButton ivNotification;

    @BindView(R.id.exported_grid)
    ViewGroup gridExported;


    @BindView(R.id.user_name)
    TextView userName;

    @OnClick(R.id.user_avatar)
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
        ExportedVideoActivity.launch(getActivity());
    }

    @OnClick(R.id.ll_uploading)
    public void onUploadClicked() {
        UploadingMomentActivity.launch(getActivity());
    }

    @OnClick(R.id.ll_my_moment)
    public void onMyMomentClicked() {
        MyMomentActivity.launch(getActivity());
    }

    @OnClick(R.id.tvNotification)
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

    @OnClick(R.id.ll_quick_start)
    public void onQuickStartClicked() {
        WebViewActivity.launch(getActivity(), WebViewActivity.PAGE_SUPPORT);
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
        EventBus.getDefault().register(mExportedAdapter);
        if (mExportedAdapter.getItemCount() == 0) {
            gridExported.setVisibility(View.GONE);
        } else {
            gridExported.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().unregister(mExportedAdapter);
    }

    private void initViews() {
        setupToolbar();
        if (SessionManager.getInstance().isLoggedIn()) {
            SessionManager sessionManager = SessionManager.getInstance();
            userAvatar.loadAvatar(sessionManager.getAvatarUrl(), sessionManager.getUserName());
            userName.setText(SessionManager.getInstance().getUserName());
        } else {
            userName.setText(R.string.click_2_login);
            userAvatar.setImageResource(R.drawable.ic_account_circle);
        }

        if (VdtCameraManager.getManager().isConnected()) {
            llCameraSetting.setVisibility(View.VISIBLE);
        } else {
            llCameraSetting.setVisibility(View.GONE);
        }

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 4);
        rvExportedVideos.setLayoutManager(layoutManager);
        mExportedAdapter = new SimpleExportedItemAdapter(getActivity());
        rvExportedVideos.setAdapter(mExportedAdapter);


        refreshLoginRelatedPrefs();

    }

    private void refreshLoginRelatedPrefs() {
        int visibility;
        if (SessionManager.getInstance().isLoggedIn()) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.GONE;
        }

        llUploading.setVisibility(visibility);
        llMyMoment.setVisibility(visibility);
        ivNotification.setVisibility(visibility);
        llWaylensCloud.setVisibility(visibility);
    }



    @Override
    public boolean onInterceptBackPressed() {
        return false;
    }

    @Override
    public void onSelected() {

    }

    @Override
    public void onDeselected() {

    }
}
