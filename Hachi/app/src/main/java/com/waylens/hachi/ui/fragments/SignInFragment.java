package com.waylens.hachi.ui.fragments;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.LoginActivity;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.OnClick;
import butterknife.OnTextChanged;

/**
 * Created by Richard on 8/17/15.
 */
public class SignInFragment extends BaseFragment {
    private static final String TAG = "SignInFragment";
    @Bind(R.id.sign_up_tabs)
    TabLayout mTabLayout;

    @Bind(R.id.sign_in_container)
    View mSignInContainer;

    @Bind(R.id.sign_up_container)
    View mSignUpContainer;

    @Bind(R.id.login_button)
    LoginButton mFBLoginButton;

    @Bind(R.id.sign_in_username)
    TextView mTvUsername;

    @Bind(R.id.sign_in_password)
    TextView mTvPassword;

    @Bind(R.id.btn_login)
    View mBtnLogin;

    @Bind(R.id.sign_in_animator)
    ViewAnimator mSignInAnimator;

    @Bind(R.id.sign_up_animator)
    ViewAnimator mSignUpAnimator;

    @Bind(R.id.sign_up_email)
    AutoCompleteTextView mTvSignUpEmail;

    CallbackManager mCallbackManager;

    RequestQueue mRequestQueue;

    ArrayAdapter<String> mAccountAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCallbackManager = CallbackManager.Factory.create();
        mRequestQueue = Volley.newRequestQueue(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_login, savedInstanceState);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    void initViews() {
        hideToolbar();
        mBtnLogin.setVisibility(View.GONE);
        String userName = SessionManager.getInstance().getUserName();
        if (!TextUtils.isEmpty(userName)) {
            mTvUsername.setText(userName);
        }
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        mSignInContainer.setVisibility(View.GONE);
                        mSignUpContainer.setVisibility(View.VISIBLE);
                        break;
                    case 1:
                        mSignUpContainer.setVisibility(View.GONE);
                        mSignInContainer.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                //
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //
            }
        });

        mTabLayout.addTab(mTabLayout.newTab().setText("SIGN UP"));
        mTabLayout.addTab(mTabLayout.newTab().setText("SIGN IN"));
        mTabLayout.getTabAt(1).select();
        mFBLoginButton.setReadPermissions("public_profile", "email", "user_friends");
        mFBLoginButton.registerCallback(mCallbackManager, new FBCallback());
        mFBLoginButton.requestFocus();
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

    @OnTextChanged({R.id.sign_in_password, R.id.sign_in_username})
    public void inputPassword(CharSequence s, int start, int before, int count) {
        if (mTvPassword.getText().length() == 0 || mTvUsername.getText().length() == 0) {
            mSignInAnimator.setDisplayedChild(0);
        } else {
            mSignInAnimator.setDisplayedChild(1);
        }
    }

    @OnClick(R.id.sign_up_next)
    public void signUp() {
        verifyEmail();
        mSignUpAnimator.setDisplayedChild(1);
    }

    private void verifyEmail() {
        String url = Constants.API_CHECK_EMAIL;
        try {
            url = url + URLEncoder.encode(mTvSignUpEmail.getText().toString(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            Logger.t(TAG).e(e, "");
        }

        mRequestQueue.add(new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                onValidEmail(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onInvalidEmail(error);
            }
        }));
    }

    void onInvalidEmail(VolleyError error) {
        SparseIntArray errorInfo = ServerMessage.parseServerError(error);
        showMessage(errorInfo.get(1));
        mSignUpAnimator.setDisplayedChild(0);
    }

    void onValidEmail(JSONObject response) {
        if (response.optBoolean("result")) {
            SignUpFragment fragment = new SignUpFragment();
            Bundle args = new Bundle();
            args.putString(SignUpFragment.ARG_KEY_EMAIL, mTvSignUpEmail.getText().toString());
            fragment.setArguments(args);
            ((LoginActivity)getActivity()).pushFragment(fragment);
        } else {
            showMessage(R.string.email_has_been_used);
            mSignUpAnimator.setDisplayedChild(0);
        }

    }

    @OnClick(R.id.btn_login)
    public void signIn() {
        mSignInAnimator.setDisplayedChild(2);

        JSONObject params = new JSONObject();
        try {
            params.put(JsonKey.USERNAME, mTvUsername.getText().toString());
            params.put(JsonKey.PASSWORD, mTvPassword.getText().toString());
        } catch (JSONException e) {
            Logger.t(TAG).e(e, "");
        }

        mRequestQueue.start();
        mRequestQueue.add(new JsonObjectRequest(Request.Method.POST, Constants.API_LOGIN, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onLoginSuccessful(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onLoginFailed(error);
                    }
                }));
    }

    void onLoginSuccessful(JSONObject response) {
        SessionManager.getInstance().saveLoginInfo(response);
        getActivity().finish();
    }

    void onLoginFailed(VolleyError error) {
        SparseIntArray errorInfo = ServerMessage.parseServerError(error);
        showMessage(errorInfo.get(1));
        if (errorInfo.get(0) == ServerMessage.USER_NAME_PASSWORD_NOT_MATCHED) {
            mTvPassword.requestFocus();
        }
        mSignInAnimator.setDisplayedChild(1);
    }

    void signUpWithFacebook(final AccessToken accessToken) {
        mRequestQueue.add(new JsonObjectRequest(Request.Method.GET, Constants.API_AUTH_FACEBOOK + accessToken.getToken(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e("test", "Response: " + response);
                        SessionManager.getInstance().saveLoginInfo(response, true);
                        if (SessionManager.getInstance().needLinkAccount()) {
                            suggestUserName(accessToken);
                        } else {
                            hideDialog();
                            getActivity().finish();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                SparseIntArray errorInfo = ServerMessage.parseServerError(error);
                showMessage(errorInfo.get(1));
                hideDialog();
            }
        }));
        mRequestQueue.start();

        showDialog();
    }

    void suggestUserName(final AccessToken accessToken) {
        GraphRequest request = GraphRequest.newMeRequest(
                accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject object,
                            GraphResponse response) {
                        Bundle args = new Bundle();
                        if (response.getError() == null) {
                            String name = object.optString("name");
                            if (!TextUtils.isEmpty(name)) {
                                name = name.replaceAll(" ", "");
                                args.putString(LinkAccountFragment.ARG_SUGGESTED_USER_NAME, name.toLowerCase());
                            }

                        }
                        hideDialog();
                        if (SessionManager.getInstance().needLinkAccount()) {
                            LinkAccountFragment fragment = new LinkAccountFragment();
                            fragment.setArguments(args);
                            ((LoginActivity) getActivity()).pushFragment(fragment);
                        } else {
                            getActivity().finish();
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name");
        request.setParameters(parameters);
        request.executeAsync();
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
            showMessage(R.string.login_error_facebook);
        }
    }

}
