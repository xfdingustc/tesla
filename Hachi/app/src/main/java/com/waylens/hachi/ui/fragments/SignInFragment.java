package com.waylens.hachi.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.LoginActivity;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONObject;

import butterknife.Bind;

/**
 * Created by Richard on 8/17/15.
 */
public class SignInFragment extends BaseFragment {
    @Bind(R.id.sign_up_tabs)
    TabLayout mTabLayout;

    @Bind(R.id.sign_in_container)
    View mSignInContainer;

    @Bind(R.id.sign_up_container)
    View mSignUpContainer;

    @Bind(R.id.login_button)
    LoginButton mFBLoginButton;

    @Bind(R.id.sign_in_username)
    TextView mUsername;

    @Bind(R.id.sign_in_password)
    TextView mPassword;

    CallbackManager mCallbackManager;

    RequestQueue mRequestQueue;

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
    }

    void signUpWithFacebook(final AccessToken accessToken) {
        mRequestQueue.add(new JsonObjectRequest(Request.Method.GET, Constants.AUTH_FACEBOOK + accessToken.getToken(),
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
                try {
                    Log.e("test", "Response: " + error);
                    if (error.networkResponse == null || error.networkResponse.data == null) {
                        showMessage(R.string.unknown_error);
                    } else {
                        String message = new String(error.networkResponse.data);
                        JSONObject jsonObject = new JSONObject(message);
                        int errorCode = jsonObject.optInt("code");
                        showMessage(ServerMessage.getErrorMessage(errorCode));
                    }
                } catch (Exception e) {
                    Log.e("test", "", e);
                }
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
