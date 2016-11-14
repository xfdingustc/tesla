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
import com.waylens.hachi.rest.IHachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.SignInPostBody;
import com.waylens.hachi.rest.body.SignUpPostBody;
import com.waylens.hachi.rest.response.AuthorizeResponse;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.views.CompoundEditView;

import org.json.JSONObject;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.adapter.rxjava.HttpException;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Richard on 8/20/15.
 */
public class SignUpFragment extends BaseFragment {

    private static final String TAG = SignUpFragment.class.getSimpleName();

    private static final String TAG_REQUEST_VERIFY_EMAIL = "SignInFragment.request.verify.email";

    private static final String TAG_REQUEST_SIGN_UP = "SignInFragment.request.sign.up";
    private static final String TAG_REQUEST_SIGN_IN = "SignInFragment.request.sign.in";

    private static final int ERROR_CODE_EMAIL_EXISTED = 31;
    private static final int ERROR_CODE_EMAIL_INVALIDE = 33;
    private static final int ERROR_CODE_PASSWORD_INVALIDE = 36;



    @BindView(R.id.button_animator)
    ViewAnimator mButtonAnimator;



    @BindView(R.id.sign_up_email)
    CompoundEditView mTvSignUpEmail;

    @BindView(R.id.sign_up_password)
    CompoundEditView mEvPassword;



    RequestQueue mVolleyRequestQueue;



    @OnClick(R.id.btn_signup)
    public void onBtnSignUpClicked() {

        if (!mTvSignUpEmail.isValid() || !mEvPassword.isValid()) {
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

        initViews();
        return view;
    }



    void initViews() {
        mRootView.requestFocus();
        String email = "";
        mTvSignUpEmail.setText(email);
    }



    private void performSignUp() {
        String email = mTvSignUpEmail.getText().toString();
        String password = mEvPassword.getText().toString();

        IHachiApi hachiApi = HachiService.createHachiApiService();
        SignUpPostBody signUpPostBody = new SignUpPostBody(email, email.substring(0, email.indexOf("@")), password);


        hachiApi.signUpRx(signUpPostBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<AuthorizeResponse>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    onSignUpFailed(e);
                }

                @Override
                public void onNext(AuthorizeResponse signUpResponse) {
                    Logger.t(TAG).d("signup response: " + signUpResponse.toString());
                    onSignUpSuccessful(signUpResponse);
                }
            });

    }

    private void onSignUpFailed(Throwable error) {
        if (error instanceof HttpException) {
            HttpException httpException = (HttpException)error;

            try {
                String errorMessage = httpException.response().errorBody().string();
                Logger.t(TAG).json(errorMessage);

                JSONObject jsonObject = new JSONObject(errorMessage);
                int code = jsonObject.getInt("code");
                switch (code) {
                    case ERROR_CODE_EMAIL_EXISTED:
                        mTvSignUpEmail.setError(getString(R.string.email_has_been_used));
                        break;
                    case ERROR_CODE_EMAIL_INVALIDE:
                        mTvSignUpEmail.setError(getString(R.string.email_error));
                        break;
                    case ERROR_CODE_PASSWORD_INVALIDE:
                        mEvPassword.setError(getString(R.string.password_error));
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mButtonAnimator.setDisplayedChild(0);
//        showMessage(ServerMessage.parseServerError(error).msgResID)
//        error.
    }

    void onSignUpSuccessful(AuthorizeResponse response) {
        performSignIn();
    }

//    private void verifyEmailOnCloud() {
//        HachiApi hachiApi = HachiService.createHachiApiService();
//        Call<SimpleBoolResponse> simpleBoolResponseCall = hachiApi.checkEmail(mEmail);
//        simpleBoolResponseCall.enqueue(new Callback<SimpleBoolResponse>() {
//            @Override
//            public void onResponse(Call<SimpleBoolResponse> call, retrofit2.Response<SimpleBoolResponse> response) {
//                onValidEmail(response.body());
//            }
//
//            @Override
//            public void onFailure(Call<SimpleBoolResponse> call, Throwable t) {
//                onInvalidEmail(t);
//
//            }
//        });
//    }

    void onInvalidEmail(Throwable error) {
        Logger.t(TAG).d(error.getMessage());
//        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
        //showMessage(errorInfo.msgResID);
        mButtonAnimator.setDisplayedChild(0);
    }

    void onValidEmail(SimpleBoolResponse response) {
        if (response.result) {
//            PreferenceUtils.putString(PreferenceUtils.KEY_SIGN_UP_EMAIL, mEmail);
//            mInputAnimator.setDisplayedChild(1);
            mButtonAnimator.setDisplayedChild(2);
        } else {
            mTvSignUpEmail.setError(getString(R.string.email_has_been_used));
            mButtonAnimator.setDisplayedChild(0);
            MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.email_has_been_used)
                    .content(R.string.sign_in_with_this_email)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            SignInFragment signInFragment = new SignInFragment();
                            Bundle mbundle = new Bundle();
//                            mbundle.putString("email", mEmail);
                            signInFragment.setArguments(mbundle);
                            AuthorizeActivity authorizeActivity = (AuthorizeActivity)getActivity();
                            authorizeActivity.switchStep(AuthorizeActivity.STEP_SIGN_IN);
                            getFragmentManager().beginTransaction().replace(R.id.fragment_content, signInFragment).commit();
                        }
                    }).show();
        }
    }

    private void performSignIn() {
        String email = mTvSignUpEmail.getText().toString();
        String password = mEvPassword.getText().toString();

        IHachiApi hachiApi = HachiService.createHachiApiService();
        SignInPostBody signInPostBody = new SignInPostBody(email, password);
        Call<AuthorizeResponse> signInResponseCall = hachiApi.signin(signInPostBody);
        signInResponseCall.enqueue(new Callback<AuthorizeResponse>() {
            @Override
            public void onResponse(Call<AuthorizeResponse> call, retrofit2.Response<AuthorizeResponse> response) {
                onSignInSuccessful(response.body());
            }

            @Override
            public void onFailure(Call<AuthorizeResponse> call, Throwable t) {
                onSignInFailed(t);
                Logger.d(t.getMessage());
            }
        });

    }

    void onSignInFailed(Throwable error) {
        Logger.d(error.getMessage(), this);
        //showMessage(ServerMessage.parseServerError(error).msgResID);
    }

    void onSignInSuccessful(AuthorizeResponse response) {
        SessionManager.getInstance().saveLoginInfo(response);
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
        Logger.d("Sign in Successful", this);
        SignUpSucceedActivity.launch(getActivity());
    }
}
