package com.waylens.hachi.ui.authorization;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.SignInPostBody;
import com.waylens.hachi.rest.response.SignInResponse;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.views.CompoundEditView;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONObject;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;


public class SignInFragment extends BaseFragment {
    private static final String TAG = SignInFragment.class.getSimpleName();

    private static final String TAG_REQUEST_SIGN_IN = "SignInFragment.request.sign.in";



    @BindView(R.id.button_animator)
    ViewAnimator mButtonAnimator;

    @BindView(R.id.sign_in_email)
    CompoundEditView mTvSignInEmail;

    @BindView(R.id.sign_in_password)
    CompoundEditView mTvPassword;

    @BindView(R.id.forgot_password_view)
    TextView mForgotPasswordView;


    RequestQueue mVolleyRequestQueue;

    PasswordTransformationMethod mTransformationMethod;

    String mEmail;
    String mPassword;

    @Override
    protected String getRequestTag() {
        return TAG;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVolleyRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
        mTransformationMethod = new PasswordTransformationMethod();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_signin, savedInstanceState);
        if(getArguments() != null)
            mEmail = getArguments().getString("email");
            mTvSignInEmail.setText(mEmail);
        Logger.t(TAG).d("Signin frament is invoked", this, mEmail);
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
    public void onStop() {
        super.onStop();
        if (mVolleyRequestQueue != null) {
            mVolleyRequestQueue.cancelAll(TAG_REQUEST_SIGN_IN);
        }
    }



    void initViews() {
        mRootView.requestFocus();
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
        HachiApi hachiApi = HachiService.createHachiApiService();
        SignInPostBody signInPostBody = new SignInPostBody(mEmail, mPassword);
        Call<SignInResponse> signInResponseCall = hachiApi.signin(signInPostBody);
        signInResponseCall.enqueue(new Callback<SignInResponse>() {
            @Override
            public void onResponse(Call<SignInResponse> call, retrofit2.Response<SignInResponse> response) {
                if (response.code() == 200) {
                    onSignInSuccessful(response.body());
                } else if(response.code() == 401) {
                    MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                            .content(R.string.incorrect_email_or_password)
                            .positiveText(android.R.string.ok)
                            .negativeText(android.R.string.cancel)
                            .show();
                    mButtonAnimator.setDisplayedChild(0);
                } else {
                    onSignInFailed(new Throwable("Sign in failed"));
                    mButtonAnimator.setDisplayedChild(0);
                }
            }

            @Override
            public void onFailure(Call<SignInResponse> call, Throwable t) {
                onSignInFailed(t);
                Logger.d(t.getMessage());
            }
        });

    }

    void onSignInFailed(Throwable error) {
        mButtonAnimator.setDisplayedChild(0);
        //showMessage(ServerMessage.parseServerError(error).msgResID);
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .content(R.string.failed_to_sign_in)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .show();
    }

    private void onSignInSuccessful(SignInResponse response) {
        SessionManager.getInstance().saveLoginInfo(response);
        doDeviceLogin();

    }

    private void doDeviceLogin() {
/*        HachiApi hachiApi = HachiService.createHachiApiService();
        DeviceLoginBody deviceLoginBody = new DeviceLoginBody("ANDROID", "xfding");
        Call<SignInResponse> deviceLoginResponseCall = hachiApi.deviceLogin(deviceLoginBody);
        deviceLoginResponseCall.enqueue(new Callback<SignInResponse>() {
            @Override
            public void onResponse(Call<SignInResponse> call, retrofit2.Response<SignInResponse> response) {
                Logger.t(TAG).d(response.body().token);
                SessionManager.getInstance().saveLoginInfo(response.body());
                getActivity().setResult(Activity.RESULT_OK);
                getActivity().finish();
            }

            @Override
            public void onFailure(Call<SignInResponse> call, Throwable t) {

            }
        });*/

        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
                .url(Constants.API_DEVICE_LOGIN)
                .postBody("deviceType", "ANDROID")
                .postBody("deviceID", "xfding")
                .listner(new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Logger.d(response.toString(), this);
                        SessionManager.getInstance().saveLoginInfo(response);
                        getActivity().setResult(Activity.RESULT_OK);
                        getActivity().finish();
                    }
                })
                .build();
        mVolleyRequestQueue.add(request);
    }
}
