package com.waylens.hachi.ui.authorization;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONObject;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2015/8/5.
 */
public class AuthorizeActivity extends BaseActivity {
    private static final String TAG = AuthorizeActivity.class.getSimpleName();

    public static final int STEP_SIGN_IN = 0;
    public static final int STEP_SIGN_UP = 1;
    public static final int STEP_FIND_PASSWORD = 2;

    private int mCurrentStep = STEP_SIGN_IN;

    private CallbackManager mCallbackManager = CallbackManager.Factory.create();

    public static void launch(Activity startActivity) {
        Intent intent = new Intent(startActivity, AuthorizeActivity.class);
        startActivity.startActivity(intent);
    }

    public static void launchForResult(Activity startActivity, int requestCode) {
        Intent intent = new Intent(startActivity, AuthorizeActivity.class);
        startActivity.startActivityForResult(intent, requestCode);
    }

    @BindView(R.id.login_button)
    LoginButton mFBLoginButton;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_authorize);
        setupToolbar();
        mFBLoginButton.setReadPermissions("public_profile", "email", "user_friends");
        mFBLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {
                Logger.t(TAG).d("on success");
                signUpWithFacebook(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Logger.t(TAG).d("on cancel");
            }

            @Override
            public void onError(FacebookException error) {
                Logger.t(TAG).d("on error");
            }
        });

        getFragmentManager().beginTransaction().replace(R.id.fragment_content, new SignInFragment()).commit();
    }





    @Override
    public void setupToolbar() {
        getToolbar().setNavigationIcon(R.drawable.navbar_close);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getToolbar().getMenu().clear();
        switch (mCurrentStep) {
            case STEP_SIGN_IN:
                setTitle(R.string.login);
                getToolbar().inflateMenu(R.menu.menu_login);
                getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.sign_up:
                                getFragmentManager().beginTransaction().replace(R.id.fragment_content, new SignUpFragment()).commit();
                                switchStep(STEP_SIGN_UP);
                                break;
                        }
                        return true;
                    }
                });
                break;
            case STEP_SIGN_UP:
                setTitle(R.string.sign_up);
                break;
            case STEP_FIND_PASSWORD:
                setTitle(R.string.forget_password);
                break;
        }




    }

    public void switchStep(int stepSignUp) {
        if (mCurrentStep == stepSignUp) {
            return;
        }
        mCurrentStep = stepSignUp;
        setupToolbar();
    }


    void onSignInSuccessful(JSONObject response) {
        SessionManager.getInstance().saveLoginInfo(response);
        doDeviceLogin();

    }

    private void doDeviceLogin() {
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_DEVICE_LOGIN)
            .postBody("deviceType", "ANDROID")
            .postBody("deviceID", "xfding")
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    SessionManager.getInstance().saveLoginInfo(response);
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            })
            .build();

        mRequestQueue.add(request);
    }

    private void signUpWithFacebook(final AccessToken accessToken) {
        Logger.t(TAG).d("get accesstoken: " + accessToken.getToken());
        mRequestQueue.add(new JsonObjectRequest(Request.Method.GET, Constants.API_AUTH_FACEBOOK + accessToken.getToken(),
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Logger.t(TAG).d("Response: " + response);
                    hideDialog();
                    onSignInSuccessful(response);
                }
            }

            , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
//                showMessage(errorInfo.msgResID);
                hideDialog();
            }
        }

        ));


        showDialog();
    }
}
