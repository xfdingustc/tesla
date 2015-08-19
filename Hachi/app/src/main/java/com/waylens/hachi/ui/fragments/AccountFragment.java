package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.LoginActivity;
import com.waylens.hachi.utils.ImageUtils;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class AccountFragment extends BaseFragment {

    @Bind(R.id.btnAvatar)
    CircleImageView mBtnAvatar;

    @Bind((R.id.login_status))
    TextView mLoginStatus;

    MaterialDialog mLogoutDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_account, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        ImageLoader.getInstance().displayImage(
                SessionManager.getInstance().getAvatarUrl(),
                mBtnAvatar,
                ImageUtils.getAvatarOptions());
        if (SessionManager.getInstance().isLoggedIn()) {
            mLoginStatus.setText(R.string.logout);
        } else {
            mLoginStatus.setText(R.string.login);
        }
    }

    @OnClick(R.id.btnAvatar)
    public void onBtnAvatarClicked() {
        if (SessionManager.getInstance().isLoggedIn()) {
            showLogoutDialog();
        } else {
            LoginActivity.launch(getActivity());
        }
    }

    void showLogoutDialog() {
        if (mLogoutDialog != null && mLogoutDialog.isShowing()) {
            return;
        }
        mLogoutDialog = new MaterialDialog.Builder(getActivity())
                .content(R.string.confirm_logout)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        logout();
                    }
                })
                .show();
    }

    void logout() {
        SessionManager.getInstance().logout();
        mBtnAvatar.setImageResource(R.drawable.sailor);
        mLoginStatus.setText(R.string.login);
    }
}
