package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.views.CompoundEditView;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Richard on 3/23/16.
 */
public class ForgotPasswordFragment extends BaseFragment {
    private static final String TAG = "ForgotPasswordFragment";

    private static final String TAG_REQUEST_SEND_EMAIL = "ForgotPasswordFragment.request.send.email";


    @Bind(R.id.sign_up_email)
    CompoundEditView mTvSignUpEmail;

    @Bind(R.id.button_animator)
    ViewAnimator mButtonAnimator;

    private String mEmail;

    private RequestQueue mVolleyRequestQueue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVolleyRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_forgot_password, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTvSignUpEmail.setText(PreferenceUtils.getString(PreferenceUtils.KEY_SIGN_UP_EMAIL, ""));
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mVolleyRequestQueue != null) {
            mVolleyRequestQueue.cancelAll(TAG_REQUEST_SEND_EMAIL);
        }
    }

    @OnClick(R.id.btn_send)
    void onClickSend() {
        if (!mTvSignUpEmail.isValid()) {
            return;
        }
        mButtonAnimator.setDisplayedChild(1);
        sendEmail();
    }

    void sendEmail() {
        String url = Constants.API_RESET_PASSWORD_MAIL;
        mEmail = mTvSignUpEmail.getText().toString();
        try {
            url = url + URLEncoder.encode(mEmail, "utf-8");
        } catch (UnsupportedEncodingException e) {
            Logger.t(TAG).e(e, "");
        }

        mVolleyRequestQueue.add(new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onSendSuccessful(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onSendFailed(error);
                    }
                }).setTag(TAG_REQUEST_SEND_EMAIL));
    }


    void onSendFailed(VolleyError error) {
        ServerMessage.ErrorMsg errorMsg = ServerMessage.parseServerError(error);
        if (errorMsg.errorCode != ServerMessage.EXCEED_MAX_RETRIES
                && errorMsg.errorCode != ServerMessage.EMAIL_TOO_FREQUENT) {
            PreferenceUtils.remove(PreferenceUtils.KEY_RESET_EMAIL_SENT);
        }
        showMessage(errorMsg.msgResID);
        mButtonAnimator.setDisplayedChild(0);
    }

    void onSendSuccessful(JSONObject response) {
        if (response.optBoolean("result", false)) {
            PreferenceUtils.putBoolean(PreferenceUtils.KEY_RESET_EMAIL_SENT, true);
            PreferenceUtils.putString(PreferenceUtils.KEY_SIGN_UP_EMAIL, mEmail);
            getFragmentManager().beginTransaction().replace(R.id.fragment_content, new ChangePasswordFragment()).commit();
        } else {
            PreferenceUtils.remove(PreferenceUtils.KEY_RESET_EMAIL_SENT);
            showMessage(R.string.server_msg_reset_email_not_sent);
            mButtonAnimator.setDisplayedChild(0);
        }
    }
}
