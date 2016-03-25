package com.waylens.hachi.ui.fragments;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.OnClick;
import butterknife.OnTextChanged;

/**
 * Created by Richard on 8/20/15.
 */
public class SignUpFragment extends BaseFragment {

    private static final String TAG = "SignUpFragment";

    private static final String TAG_REQUEST_VERIFY_EMAIL = "SignInFragment.request.verify.email";

    private static final String TAG_REQUEST_SIGN_UP = "SignInFragment.request.sign.up";
    private static final String TAG_REQUEST_SIGN_IN = "SignInFragment.request.sign.in";

    private static final int PASSWORD_MIN_LENGTH = 6;

    @Bind(R.id.login_button)
    LoginButton mFBLoginButton;

    @Bind(R.id.button_animator)
    ViewAnimator mButtonAnimator;

    @Bind(R.id.input_animator)
    ViewAnimator mInputAnimator;

    @Bind(R.id.sign_up_email)
    AutoCompleteTextView mTvSignUpEmail;

    @Bind(R.id.sign_up_password)
    AppCompatEditText mEvPassword;

    @Bind(R.id.text_input_email)
    TextInputLayout mTextInputEmail;

    @Bind(R.id.text_input_password)
    TextInputLayout mTextInputPassword;

    @Bind(R.id.btn_clean_input)
    View mBtnCleanEmail;

    @Bind(R.id.password_controls)
    View mPasswordControls;

    CallbackManager mCallbackManager;

    RequestQueue mVolleyRequestQueue;

    ArrayAdapter<String> mAccountAdapter;

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
        View view = createFragmentView(inflater, container, R.layout.fragment_signup, savedInstanceState);
        initViews();
        return view;
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
            mVolleyRequestQueue.cancelAll(TAG_REQUEST_VERIFY_EMAIL);
            mVolleyRequestQueue.cancelAll(TAG_REQUEST_SIGN_UP);
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
        mFBLoginButton.setReadPermissions("public_profile", "email", "user_friends", "user_location",
                "user_birthday");
        mFBLoginButton.registerCallback(mCallbackManager, new FBCallback());
        mBtnCleanEmail.setVisibility(View.GONE);
        mPasswordControls.setVisibility(View.GONE);
        String email = PreferenceUtils.getString(PreferenceUtils.KEY_SIGN_UP_EMAIL, null);
        if (email != null) {
            mTvSignUpEmail.setText(email);
        }
        initAccountsView();
    }

    void initAccountsView() {
        AccountManager accountManager = AccountManager.get(getActivity());
        Account[] accounts = accountManager.getAccounts();
        ArrayList<String> selectedAccounts = new ArrayList<>();
        for (Account account : accounts) {
            if (account.name.contains("@")) {
                selectedAccounts.add(account.name);
            }
        }
        mAccountAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, selectedAccounts);
        mTvSignUpEmail.setAdapter(mAccountAdapter);
        if (mAccountAdapter.getCount() > 0) {
            mTvSignUpEmail.setText(mAccountAdapter.getItem(0));
        }
    }

    @OnClick(R.id.btn_sign_in)
    void onClickSignIn() {
        getActivity().setTitle(R.string.login);
        getFragmentManager().beginTransaction().replace(R.id.fragment_content, new SignInFragment()).commit();
    }

    @OnClick(R.id.btn_clean_input)
    void cleanInputText() {
        mTvSignUpEmail.getText().clear();
    }

    @OnClick(R.id.btn_clean_password)
    void cleanPassword() {
        mEvPassword.getText().clear();
    }

    @OnClick(R.id.btn_show_password)
    void showPassword(View view) {
        view.setSelected(!view.isSelected());
        mEvPassword.setTransformationMethod(view.isSelected() ? null : mTransformationMethod);
    }

    @OnClick(R.id.sign_up_next)
    public void signUpNext() {
        String email = mTvSignUpEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mTextInputEmail.setError(getString(R.string.the_field_is_required));
            return;
        }

        boolean isValid = Patterns.EMAIL_ADDRESS.matcher(email).matches();
        if (!isValid) {
            mTextInputEmail.setError(getString(R.string.email_invalid));
            return;
        }
        mTextInputEmail.setError(null);
        verifyEmail();
        mButtonAnimator.setDisplayedChild(1);
    }

    @OnTextChanged(R.id.sign_up_email)
    public void inputEmail(CharSequence text) {
        mTextInputEmail.setError(null);
        mBtnCleanEmail.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }

    @OnTextChanged(R.id.sign_up_password)
    public void inputPassword(CharSequence text) {
        mPasswordControls.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }

    @OnClick(R.id.sign_up_done)
    void signUpDone() {
        mPassword = mEvPassword.getText().toString();
        if (TextUtils.isEmpty(mPassword) || mPassword.length() < PASSWORD_MIN_LENGTH) {
            mTextInputPassword.setError(getString(R.string.password_min_error, PASSWORD_MIN_LENGTH));
            return;
        }
        mEvPassword.setError(null);
        mButtonAnimator.setDisplayedChild(1);

        mEmail = mTvSignUpEmail.getText().toString();
        performSignUp();
    }

    void performSignUp() {
        JSONObject params = new JSONObject();
        try {
            params.put(JsonKey.EMAIL, mEmail);
            params.put(JsonKey.USERNAME, mEmail.substring(0, mEmail.indexOf("@")));
            params.put(JsonKey.PASSWORD, mPassword);
        } catch (JSONException e) {
            Logger.t(TAG).e(e, "");
        }
        mVolleyRequestQueue.add(new JsonObjectRequest(Request.Method.POST, Constants.API_SIGN_UP, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onSignUpSuccessful(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onSignUpFailed(error);
                    }
                }).setTag(TAG_REQUEST_SIGN_UP));
    }

    void onSignUpFailed(VolleyError error) {
        showMessage(ServerMessage.parseServerError(error).msgResID);
    }

    void onSignUpSuccessful(JSONObject response) {
        performSignIn();
    }

    private void verifyEmail() {
        String url = Constants.API_CHECK_EMAIL;
        final String email = mTvSignUpEmail.getText().toString();
        try {
            url = url + URLEncoder.encode(email, "utf-8");
        } catch (UnsupportedEncodingException e) {
            Logger.t(TAG).e(e, "");
        }

        mVolleyRequestQueue.add(new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                onValidEmail(email, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onInvalidEmail(error);
            }
        }).setTag(TAG_REQUEST_VERIFY_EMAIL));
    }

    void onInvalidEmail(VolleyError error) {
        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
        showMessage(errorInfo.msgResID);
        mButtonAnimator.setDisplayedChild(0);
    }

    void onValidEmail(String email, JSONObject response) {
        if (response.optBoolean("result")) {
            PreferenceUtils.putString(PreferenceUtils.KEY_SIGN_UP_EMAIL, email);
            mInputAnimator.setDisplayedChild(1);
            mButtonAnimator.setDisplayedChild(2);
        } else {
            mTextInputEmail.setError(getString(R.string.email_has_been_used));
            mButtonAnimator.setDisplayedChild(0);
        }
    }

    void performSignIn() {
        JSONObject params = new JSONObject();
        try {
            params.put(JsonKey.EMAIL, mEmail);
            params.put(JsonKey.PASSWORD, mPassword);
        } catch (JSONException e) {
            Logger.t(TAG).e(e, "");
        }
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
        showMessage(ServerMessage.parseServerError(error).msgResID);
    }

    void onSignInSuccessful(JSONObject response) {
        SessionManager.getInstance().saveLoginInfo(response);
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    void signUpWithFacebook(final AccessToken accessToken) {
        mVolleyRequestQueue.add(new JsonObjectRequest(Request.Method.GET, Constants.API_AUTH_FACEBOOK + accessToken.getToken(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e("test", "Response: " + response);
                        SessionManager.getInstance().saveLoginInfo(response, true);
                        hideDialog();
                        getActivity().setResult(Activity.RESULT_OK);
                        getActivity().finish();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                showMessage(errorInfo.msgResID);
                hideDialog();
            }
        }));
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
            Log.e("test", "facebook login", e);
            showMessage(R.string.login_error_facebook);
        }
    }
}
