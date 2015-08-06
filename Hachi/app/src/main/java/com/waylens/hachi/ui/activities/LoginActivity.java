package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.session.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2015/8/5.
 */
public class LoginActivity extends BaseActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();

    private MaterialDialog mLoginProgressDialog;

    @Bind(R.id.etUsername)
    EditText mEtUsername;

    @Bind(R.id.etPassword)
    EditText mEtPassword;

    @Bind(R.id.btnLogin)
    Button mBtnLogin;

    @Bind(R.id.btnForgetPassword)
    Button mForgetPassword;

    @OnClick(R.id.btnLogin)
    public void onBtnLoginClicked() {
        if (!loginValidate()) {
            return;
        }
        String url = Constants.HOST_URL + Constants.LOGIN_URL;

        Map<String, String> login_params = new HashMap<>();
        login_params.put(JsonKey.USERNAME, mEtUsername.getText().toString());
        login_params.put(JsonKey.PASSWORD, mEtPassword.getText().toString());
        JSONObject body = new JSONObject(login_params);

        mRequestQueue.add(new JsonObjectRequest(Request.Method.POST, url, body,
            new LoginResponseListener(), new LoginErrorListener()));
        mRequestQueue.start();

        mLoginProgressDialog = new MaterialDialog.Builder(this)
            .title(R.string.login)
            .progress(true, 0)
            .progressIndeterminateStyle(false)
            .build();

        mLoginProgressDialog.show();
    }


    @OnClick(R.id.btnHaveNoAccount)
    public void onBtnHaveNoAccountClicked() {
        SignupActivity.launch(this);
        finish();
    }

    public static void launch(Activity startActivity) {
        Intent intent = new Intent(startActivity, LoginActivity.class);
        startActivity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_login);

    }


    public boolean loginValidate() {
        boolean valid = true;

        String userName = mEtUsername.getText().toString();
        String passWord = mEtPassword.getText().toString();

        if (userName.isEmpty()) {
            mEtUsername.setError(getString(R.string.username_error));
            valid = false;
        } else {
            mEtUsername.setError(null);
        }

        if (passWord.isEmpty()) {
            mEtPassword.setError(getString(R.string.password_error));
            valid = false;
        } else {
            mEtPassword.setError(null);
        }

        return valid;
    }

    private class LoginResponseListener implements Response.Listener<JSONObject> {

        @Override
        public void onResponse(JSONObject response) {
            Logger.json(response.toString());

            SessionManager sm = SessionManager.getInstance();
            String oldUserId = sm.getUserId();

            // First save the login info into shared preference
            SessionManager.getInstance().saveLoginInfo(response);


            SessionManager.getInstance().refreshUserProfile();

            // Finally show the login successfully toast
            onLoginSuccessfully();
            //finish();
        }
    }


    private class LoginErrorListener implements Response.ErrorListener {

        @Override
        public void onErrorResponse(VolleyError error) {
            onLoginFailed(error);
        }
    }

    private void onLoginSuccessfully() {
        Snackbar.make(mBtnLogin, getString(R.string.login_successfully), Snackbar.LENGTH_LONG).show();
        mLoginProgressDialog.dismiss();
        finish();
    }

    private void onLoginFailed(VolleyError error) {
        String errorMessageJson = new String(error.networkResponse.data);

        String errorMessage = getString(R.string.unknown_error);


        try {
            JSONObject errorJson = new JSONObject(errorMessageJson);
            Logger.t(TAG).json(errorJson.toString());
            int code = errorJson.getInt("code");
            switch (code) {
                case 20:
                    errorMessage = getString(R.string.user_name_or_password_error);
                    mEtUsername.setText("");
                    mEtPassword.setText("");
                    break;
                default:
                    errorMessage = errorJson.getString("msg");
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Snackbar.make(mBtnLogin, errorMessage, Snackbar.LENGTH_LONG).show();
        mLoginProgressDialog.dismiss();
    }
}
