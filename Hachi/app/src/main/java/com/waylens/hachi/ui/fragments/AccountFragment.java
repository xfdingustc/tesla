package com.waylens.hachi.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.gcm.RegistrationIntentService;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.LoginActivity;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.PushUtils;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONObject;

import butterknife.Bind;
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

    @Override
    public void onResume() {
        super.onResume();
        if (SessionManager.getInstance().isLoggedIn()
                && PreferenceUtils.getString(PreferenceUtils.SEND_GCM_TOKEN_SERVER, null) == null
                && PushUtils.checkGooglePlayServices(getActivity())) {
            Intent intent = new Intent(getActivity(), RegistrationIntentService.class);
            getActivity().startService(intent);
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
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        requestQueue.start();
        requestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, Constants.API_DEVICE_DEACTIVATION,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response.optBoolean("result")) {
                            PreferenceUtils.remove(PreferenceUtils.SEND_GCM_TOKEN_SERVER);
                            SessionManager.getInstance().logout();
                            mBtnAvatar.setImageResource(R.drawable.sailor);
                            mLoginStatus.setText(R.string.login);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerMessage.ErrorMsg errorMsg = ServerMessage.parseServerError(error);
                        showMessage(errorMsg.msgResID);
                    }
                }));
    }
}
