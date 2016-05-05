package com.waylens.hachi.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
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
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.views.CompoundEditView;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.OnClick;


public class SignInFragment extends BaseFragment {
    private static final String TAG = SignInFragment.class.getSimpleName();

    private static final String TAG_REQUEST_SIGN_IN = "SignInFragment.request.sign.in";

    @BindView(R.id.login_button)
    LoginButton mFBLoginButton;

    @BindView(R.id.button_animator)
    ViewAnimator mButtonAnimator;

    @BindView(R.id.sign_in_email)
    CompoundEditView mTvSignInEmail;

    @BindView(R.id.sign_in_password)
    CompoundEditView mTvPassword;

    @BindView(R.id.forgot_password_view)
    TextView mForgotPasswordView;

    CallbackManager mCallbackManager;

    RequestQueue mVolleyRequestQueue;

    PasswordTransformationMethod mTransformationMethod;

    String mEmail;
    String mPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCallbackManager = CallbackManager.Factory.create();
        mVolleyRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
        mTransformationMethod = new PasswordTransformationMethod();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_signin, savedInstanceState);
        initViews();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SpannableStringBuilder ssb = new SpannableStringBuilder(getString(R.string.forget_password_hint1));
        int start = ssb.length();
        ssb.append(getString(R.string.forgot_password_hint2))
            .setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    onForgotPassword();
                }
            }, start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mForgotPasswordView.setText(ssb);
        mForgotPasswordView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    void onForgotPassword() {
        Fragment fragment;
        if (PreferenceUtils.getBoolean(PreferenceUtils.KEY_RESET_EMAIL_SENT, false)) {
            getActivity().setTitle(R.string.change_password);
            fragment = new ChangePasswordFragment();
        } else {
            getActivity().setTitle(R.string.forget_password);
            fragment = new ForgotPasswordFragment();
        }
        getFragmentManager().beginTransaction().replace(R.id.fragment_content, fragment).commit();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (TextUtils.isEmpty(SessionManager.getInstance().getToken())
            && AccessToken.getCurrentAccessToken() != null
            && !AccessToken.getCurrentAccessToken().isExpired()) {
            signUpWithFacebook(AccessToken.getCurrentAccessToken());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mVolleyRequestQueue != null) {
            mVolleyRequestQueue.cancelAll(TAG_REQUEST_SIGN_IN);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    void initViews() {
        mRootView.requestFocus();
        mFBLoginButton.setReadPermissions("public_profile", "email", "user_friends");
        mFBLoginButton.registerCallback(mCallbackManager, new FBCallback());
        String email = PreferenceUtils.getString(PreferenceUtils.KEY_SIGN_UP_EMAIL, null);
        if (email != null) {
            mTvSignInEmail.setText(email);
        }
    }

    @OnClick(R.id.btn_sign_in)
    public void onClickSignIn() {
        if (!mTvSignInEmail.isValid() || !mTvPassword.isValid()) {
            return;
        }
        mButtonAnimator.setDisplayedChild(1);
        performSignIn();
    }

    void performSignIn() {
        mEmail = mTvSignInEmail.getText().toString();
        mPassword = mTvPassword.getText().toString();
        JSONObject params = new JSONObject();
        try {
            params.put(JsonKey.EMAIL, mEmail);
            params.put(JsonKey.PASSWORD, mPassword);
        } catch (JSONException e) {
            Logger.t(TAG).e(e, "");
        }
        Logger.t(TAG).d("signin : " + params.toString());
        mVolleyRequestQueue.add(new JsonObjectRequest(Request.Method.POST, Constants.API_SIGN_IN, params,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    onSignInSuccessful(response);
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    onSignInFailed(error);
                }
            }).setTag(TAG_REQUEST_SIGN_IN));
    }

    void onSignInFailed(VolleyError error) {
        mButtonAnimator.setDisplayedChild(0);
        showMessage(ServerMessage.parseServerError(error).msgResID);
    }

    void onSignInSuccessful(JSONObject response) {
        SessionManager.getInstance().saveLoginInfo(response);
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    private void signUpWithFacebook(final AccessToken accessToken) {
        Logger.t(TAG).d("get accesstoken: " + accessToken.getToken());
        mVolleyRequestQueue.add(new JsonObjectRequest(Request.Method.GET, Constants.API_AUTH_FACEBOOK + accessToken.getToken(),
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Logger.t(TAG).d("Response: " + response);
                    SessionManager.getInstance().saveLoginInfo(response, true);
                    hideDialog();
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                }
            }

            , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                showMessage(errorInfo.msgResID);
                hideDialog();
            }
        }

        ));
        mVolleyRequestQueue.start();

        showDialog();
    }

    class FBCallback implements FacebookCallback<LoginResult> {
        @Override
        public void onSuccess(LoginResult loginResult) {
            signUpWithFacebook(loginResult.getAccessToken());
        }

        @Override
        public void onCancel() {
            showMessage(R.string.login_cancelled);
        }

        @Override
        public void onError(FacebookException e) {
            Logger.t(TAG).d("facebook login", e);
            showMessage(R.string.login_error_facebook);
        }
    }
}
