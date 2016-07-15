package com.waylens.hachi.ui.authorization;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.SignInPostBody;
import com.waylens.hachi.rest.body.SignUpPostBody;
import com.waylens.hachi.rest.response.SignInResponse;
import com.waylens.hachi.rest.response.SignUpResponse;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.views.CompoundEditView;
import com.waylens.hachi.utils.PreferenceUtils;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * Created by Richard on 8/20/15.
 */
public class SignUpFragment extends BaseFragment {

    private static final String TAG = SignUpFragment.class.getSimpleName();

    private static final String TAG_REQUEST_VERIFY_EMAIL = "SignInFragment.request.verify.email";

    private static final String TAG_REQUEST_SIGN_UP = "SignInFragment.request.sign.up";
    private static final String TAG_REQUEST_SIGN_IN = "SignInFragment.request.sign.in";



    @BindView(R.id.button_animator)
    ViewAnimator mButtonAnimator;

    @BindView(R.id.input_animator)
    ViewAnimator mInputAnimator;

    @BindView(R.id.sign_up_email)
    CompoundEditView mTvSignUpEmail;

    @BindView(R.id.sign_up_password)
    CompoundEditView mEvPassword;



    RequestQueue mVolleyRequestQueue;

    String mEmail;
    String mPassword;



    @OnClick(R.id.btn_sign_in)
    void onClickSignIn() {
        AuthorizeActivity authorizeActivity = (AuthorizeActivity)getActivity();
        authorizeActivity.switchStep(AuthorizeActivity.STEP_SIGN_IN);
        getFragmentManager().beginTransaction().replace(R.id.fragment_content, new SignInFragment()).commit();

    }

    @OnClick(R.id.sign_up_next)
    public void signUpNext() {
        mEmail = mTvSignUpEmail.getText().toString();
        if (!mTvSignUpEmail.isValid()) {
            return;
        }
        verifyEmailOnCloud();
        mButtonAnimator.setDisplayedChild(1);
    }

    @OnClick(R.id.sign_up_done)
    void signUpDone() {
        mPassword = mEvPassword.getText().toString();
        if (!mEvPassword.isValid()) {
            return;
        }
        mButtonAnimator.setDisplayedChild(1);
        performSignUp();
    }


    @Override
    protected String getRequestTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVolleyRequestQueue = Volley.newRequestQueue(getActivity());
        mVolleyRequestQueue.start();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_signup, savedInstanceState);
        if(getArguments() != null)
            mEmail = getArguments().getString("email");
        mTvSignUpEmail.setText(mEmail);
        Logger.t(TAG).d("Signup frament is invoked", this, mEmail);
        initViews();
        return view;
    }



    void initViews() {
        mRootView.requestFocus();
        String email = null;
        if(mEmail == null)
            email = PreferenceUtils.getString(PreferenceUtils.KEY_SIGN_UP_EMAIL, null);
        if (email != null) {
            mTvSignUpEmail.setText(email);
        }
    }



    void performSignUp() {

        HachiApi hachiApi = HachiService.createHachiApiService();
        SignUpPostBody signUpPostBody = new SignUpPostBody(mEmail, mEmail.substring(0, mEmail.indexOf("@")), mPassword);
        Call<SignUpResponse> signUpResponseCall = hachiApi.signUp(signUpPostBody);
        signUpResponseCall.enqueue(new Callback<SignUpResponse>() {
            @Override
            public void onResponse(Call<SignUpResponse> call, retrofit2.Response<SignUpResponse> response) {
                onSignUpSuccessful(response.body());
            }

            @Override
            public void onFailure(Call<SignUpResponse> call, Throwable t) {
                onSignUpFailed(t);
            }
        });

    }

    void onSignUpFailed(Throwable error) {
        Logger.d(error.getMessage(), this);
        //showMessage(ServerMessage.parseServerError(error).msgResID);
    }

    void onSignUpSuccessful(SignUpResponse response) {
        performSignIn();
    }

    private void verifyEmailOnCloud() {
        HachiApi hachiApi = HachiService.createHachiApiService();
        Call<SimpleBoolResponse> simpleBoolResponseCall = hachiApi.checkEmail(mEmail);
        simpleBoolResponseCall.enqueue(new Callback<SimpleBoolResponse>() {
            @Override
            public void onResponse(Call<SimpleBoolResponse> call, retrofit2.Response<SimpleBoolResponse> response) {
                onValidEmail(response.body());
            }

            @Override
            public void onFailure(Call<SimpleBoolResponse> call, Throwable t) {
                onInvalidEmail(t);

            }
        });
    }

    void onInvalidEmail(Throwable error) {
        Logger.d(error.getMessage(), this);
        //ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
        //showMessage(errorInfo.msgResID);
        mButtonAnimator.setDisplayedChild(0);
    }

    void onValidEmail(SimpleBoolResponse response) {
        if (response.result) {
            PreferenceUtils.putString(PreferenceUtils.KEY_SIGN_UP_EMAIL, mEmail);
            mInputAnimator.setDisplayedChild(1);
            mButtonAnimator.setDisplayedChild(2);
        } else {
            mTvSignUpEmail.setError(getString(R.string.email_has_been_used));
            mButtonAnimator.setDisplayedChild(0);
            MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.email_has_been_used)
                    .content(R.string.sign_in_with_this_email)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            SignInFragment signInFragment = new SignInFragment();
                            Bundle mbundle = new Bundle();
                            mbundle.putString("email", mEmail);
                            signInFragment.setArguments(mbundle);
                            AuthorizeActivity authorizeActivity = (AuthorizeActivity)getActivity();
                            authorizeActivity.switchStep(AuthorizeActivity.STEP_SIGN_IN);
                            getFragmentManager().beginTransaction().replace(R.id.fragment_content, signInFragment).commit();
                        }
                    }).show();
        }
    }

    void performSignIn() {

        HachiApi hachiApi = HachiService.createHachiApiService();
        SignInPostBody signInPostBody = new SignInPostBody(mEmail, mPassword);
        Call<SignInResponse> signInResponseCall = hachiApi.signin(signInPostBody);
        signInResponseCall.enqueue(new Callback<SignInResponse>() {
            @Override
            public void onResponse(Call<SignInResponse> call, retrofit2.Response<SignInResponse> response) {
                onSignInSuccessful(response.body());
            }

            @Override
            public void onFailure(Call<SignInResponse> call, Throwable t) {
                onSignInFailed(t);
                Logger.d(t.getMessage());
            }
        });

    }

    void onSignInFailed(Throwable error) {
        Logger.d(error.getMessage(), this);
        //showMessage(ServerMessage.parseServerError(error).msgResID);
    }

    void onSignInSuccessful(SignInResponse response) {
        SessionManager.getInstance().saveLoginInfo(response);
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
        Logger.d("Sign in Successful", this);
        SignUpSucceedActivity.launch(getActivity());
    }
}
