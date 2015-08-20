package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Richard on 8/20/15.
 */
public class SignUpFragment extends BaseFragment {

    private static final String TAG = "SignUpFragment";

    public static final String ARG_KEY_EMAIL = "arg.sign.up.email";

    String mPreSetEmail;

    @Bind(R.id.sign_up_email)
    EditText mEtEmail;

    @Bind(R.id.sign_up_animator)
    ViewAnimator mSignUpAnimator;

    @Bind(R.id.sign_up_user_name)
    EditText mEtUserName;

    @Bind(R.id.sign_up_password)
    EditText mEtPassword;

    private RequestQueue mRequestQueue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mPreSetEmail = args.getString(ARG_KEY_EMAIL, null);
        }
        mRequestQueue = Volley.newRequestQueue(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_signup, savedInstanceState);
        initViews();
        return view;
    }

    private void initViews() {
        if (!TextUtils.isEmpty(mPreSetEmail)) {
            mEtEmail.setText(mPreSetEmail);
            mEtEmail.setTextColor(getResources().getColor(R.color.material_green_600));
        }
    }
    
    @OnClick(R.id.sign_up_next)
    public void signUp() {
        mSignUpAnimator.setDisplayedChild(1);

        JSONObject params = new JSONObject();
        try {
            params.put(JsonKey.USERNAME, mEtUserName.getText().toString());
            params.put(JsonKey.EMAIL, mEtEmail.getText().toString());
            params.put(JsonKey.PASSWORD, mEtPassword.getText().toString());
        } catch (JSONException e) {
            Logger.t(TAG).e(e, "");
        }
        mRequestQueue.start();
        mRequestQueue.add(new JsonObjectRequest(Request.Method.POST, Constants.API_SIGN_UP, params,
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
                }));
    }

    void onSignUpSuccessful(JSONObject response) {
        SessionManager.getInstance().saveLoginInfo(response);
        getActivity().finish();
    }

    void onSignUpFailed(VolleyError error) {
        SparseIntArray errorInfo = ServerMessage.parseServerError(error);
        showMessage(errorInfo.get(1));
        mSignUpAnimator.setDisplayedChild(0);
    }
}
