package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

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
import java.util.Map;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2015/8/5.
 */
public class SignupActivity extends BaseActivity {
    private static final String TAG = SignupActivity.class.getSimpleName();

    private MaterialDialog mSignupProgressDialog;

    @Bind(R.id.etUsernameSignup)
    EditText mEtUsername;

    @Bind(R.id.etPasswordSignup)
    EditText mEtPassword;

    @Bind(R.id.etEmailSignup)
    EditText mEtEmail;

    @Bind(R.id.btnSignup)
    Button mBtnSignup;


    @OnClick(R.id.btnHaveAccount)
    public void onBtnHaveAccountClicked() {
        LoginActivity.launch(this);
        finish();
    }

    @OnClick(R.id.btnSignup)
    public void onBtnSignupClicked() {
        if (!signupValidate()) {
            return;
        }

        String url = Constants.HOST_URL + Constants.SIGN_UP;

        Map<String, String> register_params = new HashMap<>();
        register_params.put(JsonKey.USERNAME, mEtUsername.getText().toString());
        register_params.put(JsonKey.EMAIL, mEtEmail.getText().toString());
        register_params.put(JsonKey.PASSWORD, mEtPassword.getText().toString());
        JSONObject body = new JSONObject(register_params);

        mRequestQueue.add(new JsonObjectRequest(Request.Method.POST, url, body,
            new SignupResponseListener(), new SignupErrorListener()));
        mRequestQueue.start();

        mSignupProgressDialog = new MaterialDialog.Builder(this)
            .title(R.string.signup)
            .progress(true, 0)
            .progressIndeterminateStyle(false)
            .show();
    }

    public static void launch(Activity startActivity) {
        Intent intent = new Intent(startActivity, SignupActivity.class);
        startActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_signup);
    }

    public boolean signupValidate() {
        boolean valid = true;

        String userName = mEtUsername.getText().toString();
        String email = mEtEmail.getText().toString();
        String password = mEtPassword.getText().toString();

        if (userName.isEmpty()) {
            mEtUsername.setError(getString(R.string.username_error));
            valid = false;
        } else {
            mEtUsername.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEtEmail.setError(getString(R.string.email_error));
            valid = false;
        } else {
            mEtEmail.setError(null);
        }

        if (password.isEmpty()) {
            mEtPassword.setError(getString(R.string.password_error));
            valid = false;
        } else {
            mEtPassword.setError(null);
        }

        return valid;
    }

    private class SignupResponseListener implements Response.Listener<JSONObject> {

        @Override
        public void onResponse(JSONObject response) {
            Logger.t(TAG).json(response.toString());
            SessionManager.getInstance().saveLoginInfo(response);
            onSignupSuccessfully();
        }
    }

    private class SignupErrorListener implements Response.ErrorListener {

        @Override
        public void onErrorResponse(VolleyError error) {
            onSignupFailed(error);
        }
    }

    private void onSignupSuccessfully() {
        //SessionManager.getInstance().refreshUserProfile();
        Snackbar.make(mBtnSignup, getString(R.string.signup_successfully), Snackbar.LENGTH_LONG)
            .show();
        mSignupProgressDialog.dismiss();
        finish();
    }

    private void onSignupFailed(VolleyError error) {
        String errorMessageJson = new String(error.networkResponse.data);

        String errorMessage = getString(R.string.unknown_error);

        try {
            JSONObject errorJson = new JSONObject(errorMessageJson);
            int code = errorJson.getInt("code");
            switch (code) {
                case 30:
                    errorMessage = getString(R.string.user_name_exist);
                    mEtUsername.setText("");
                    mEtUsername.setError(errorMessage);
                    break;
                default:
                    errorMessage = errorJson.getString("msg");
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Snackbar.make(mBtnSignup, errorMessage, Snackbar.LENGTH_LONG).show();
        mSignupProgressDialog.dismiss();
    }
}
