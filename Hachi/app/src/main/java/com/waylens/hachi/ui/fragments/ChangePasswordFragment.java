package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatEditText;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
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
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Richard on 3/23/16.
 */
public class ChangePasswordFragment extends BaseFragment {
    private static final String TAG = "ChangePasswordFragment";

    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int CODE_MIN_LENGTH = 6;

    private static final String TAG_REQUEST_RESET_PASSWORD = "request.reset.password";


    @Bind(R.id.text_input_code)
    TextInputLayout mTextInputCode;

    @Bind(R.id.forgot_password_code)
    AppCompatEditText mEvCode;

    @Bind(R.id.text_input_password)
    TextInputLayout mTextInputPassword;

    @Bind(R.id.sign_up_password)
    AppCompatEditText mEvPassword;

    @Bind(R.id.button_animator)
    ViewAnimator mButtonAnimator;

    @Bind(R.id.tv_resend)
    TextView mTvResend;

    @Bind(R.id.tv_change_password_hint)
    TextView mTvChangePasswordHint;

    private String mPassword;
    private String mCode;
    private RequestQueue mVolleyRequestQueue;
    private String mEmail;


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
        mTvChangePasswordHint.setText(getString(R.string.forgot_password_hint4, mEmail));
        SpannableStringBuilder ssb = new SpannableStringBuilder(getString(R.string.forgot_password_hint5));
        int start = ssb.length();
        ssb.append(getString(R.string.forgot_password_hint6))
                .setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        onResendEmail();
                    }
                }, start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mTvResend.setText(ssb);
        mTvResend.setMovementMethod(LinkMovementMethod.getInstance());

    }

    void onResendEmail() {
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
        if (!validateCode() || !validatePassword()) {
            return;
        }
        mButtonAnimator.setDisplayedChild(1);
        changePassword();
    }

    void changePassword() {
        JSONObject params = new JSONObject();
        try {
            params.put(JsonKey.EMAIL, mEmail);
            params.put(JsonKey.TOKEN, mCode);
            params.put(JsonKey.NEW_PASSWORD, mPassword);
        } catch (JSONException e) {
            Logger.t(TAG).e(e, "");
        }
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

    boolean validateCode() {
        mCode = mEvCode.getText().toString();
        if (TextUtils.isEmpty(mCode) || mCode.length() < PASSWORD_MIN_LENGTH) {
            mTextInputCode.setError(getString(R.string.code_min_error, CODE_MIN_LENGTH));
            return false;
        }
        mTextInputCode.setError(null);
        return true;
    }

    boolean validatePassword() {
        mPassword = mEvPassword.getText().toString();
        if (TextUtils.isEmpty(mPassword) || mPassword.length() < PASSWORD_MIN_LENGTH) {
            mTextInputPassword.setError(getString(R.string.password_min_error, PASSWORD_MIN_LENGTH));
            return false;
        }
        mEvPassword.setError(null);
        return true;
    }


}
