package com.waylens.hachi.ui.authorization;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.afollestad.materialdialogs.MaterialDialog;
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
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.views.CompoundEditView;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import butterknife.BindView;
import butterknife.OnClick;

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

        String email = PreferenceUtils.getString(PreferenceUtils.KEY_SIGN_UP_EMAIL, null);
        if (email != null) {
            mTvSignUpEmail.setText(email);
        }
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

    private void verifyEmailOnCloud() {
        String url = Constants.API_CHECK_EMAIL;
        try {
            url = url + URLEncoder.encode(mEmail, "utf-8");
        } catch (UnsupportedEncodingException e) {
            Logger.t(TAG).e(e, "");
        }

        mVolleyRequestQueue.add(new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                onValidEmail(response);
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

    void onValidEmail(JSONObject response) {
        if (response.optBoolean("result")) {
            PreferenceUtils.putString(PreferenceUtils.KEY_SIGN_UP_EMAIL, mEmail);
            mInputAnimator.setDisplayedChild(1);
            mButtonAnimator.setDisplayedChild(2);
        } else {
            mTvSignUpEmail.setError(getString(R.string.email_has_been_used));
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





}
