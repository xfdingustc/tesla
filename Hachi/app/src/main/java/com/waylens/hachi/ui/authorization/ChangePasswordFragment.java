package com.waylens.hachi.ui.authorization;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.views.CompoundEditView;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONException;
import org.json.JSONObject;


import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Richard on 3/23/16.
 */
public class ChangePasswordFragment extends BaseFragment {
    private static final String TAG = ChangePasswordFragment.class.getSimpleName();

    private static final String TAG_REQUEST_RESET_PASSWORD = "request.reset.password";

    @BindView(R.id.forgot_password_code)
    CompoundEditView mEvCode;

    @BindView(R.id.new_password)
    CompoundEditView mEvPassword;

    @BindView(R.id.button_animator)
    ViewAnimator mButtonAnimator;

    @BindView(R.id.tv_resend)
    TextView mTvResend;

    @BindView(R.id.tv_change_password_hint)
    TextView mTvChangePasswordHint;

    private String mPassword;
    private String mCode;
    private RequestQueue mVolleyRequestQueue;
    private String mEmail;


    @Override
    protected String getRequestTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVolleyRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
        mEmail = PreferenceUtils.getString(PreferenceUtils.KEY_SIGN_UP_EMAIL, "");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_change_password, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTvChangePasswordHint.setText(getString(R.string.forgot_password_hint4) + " " + mEmail);
        SpannableStringBuilder ssb = new SpannableStringBuilder(getString(R.string.forgot_password_hint5));
        int start = ssb.length();
        ssb.append(getString(R.string.resend))
                .setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        onResendEmail();
                    }
                }, start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mTvResend.setText(ssb);
        mTvResend.setMovementMethod(LinkMovementMethod.getInstance());

    }

    private void onResendEmail() {
        getFragmentManager().beginTransaction().replace(R.id.fragment_content, new ForgotPasswordFragment()).commit();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mVolleyRequestQueue != null) {
            mVolleyRequestQueue.cancelAll(TAG_REQUEST_RESET_PASSWORD);
        }
    }

    @OnClick(R.id.btn_change_password)
    void onClickChangePassword() {
        mCode = mEvCode.getText().toString();
        mPassword = mEvPassword.getText().toString();

        if (!mEvCode.isValid() || !mEvPassword.isValid()) {
            return;
        }
        mButtonAnimator.setDisplayedChild(1);
        changePassword();
    }

    private void changePassword() {
        JSONObject params = new JSONObject();
        try {
            params.put(JsonKey.EMAIL, mEmail);
            params.put(JsonKey.TOKEN, mCode);
            params.put(JsonKey.NEW_PASSWORD, mPassword);
        } catch (JSONException e) {
            Logger.t(TAG).e(e, "");
        }

        Logger.t(TAG).d("reset password: " + params.toString());
        mVolleyRequestQueue.add(new JsonObjectRequest(Request.Method.POST, Constants.API_RESET_PASSWORD, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onResetPasswordSuccessful(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onResetPasswordFailed(error);
                    }
                }).setTag(TAG_REQUEST_RESET_PASSWORD));
    }

    private void onResetPasswordFailed(VolleyError error) {
        showMessage(ServerMessage.parseServerError(error).msgResID);
        mButtonAnimator.setDisplayedChild(0);
    }

    private void onResetPasswordSuccessful(JSONObject response) {
        PreferenceUtils.remove(PreferenceUtils.KEY_RESET_EMAIL_SENT);
        if (response.optBoolean("result", false)) {
            getFragmentManager().beginTransaction().replace(R.id.fragment_content, new SignInFragment()).commit();
        } else {
            mButtonAnimator.setDisplayedChild(0);
        }
    }

}
