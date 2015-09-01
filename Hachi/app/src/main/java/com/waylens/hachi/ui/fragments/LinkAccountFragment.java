package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.utils.ServerMessage;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Richard on 8/17/15.
 */
public class LinkAccountFragment extends BaseFragment {
    private static final String TAG = "Waylens";

    public static final String ARG_SUGGESTED_USER_NAME = "arg.suggested.user.name";

    @Bind(R.id.user_avatar)
    ImageView mAvatar;

    @Bind(R.id.sign_in_username)
    TextView mUserName;

    @Bind(R.id.sign_in_password)
    TextView mPassword;

    @Bind(R.id.control_container)
    ViewAnimator mViewAnimator;

    private RequestQueue mRequestQueue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = Volley.newRequestQueue(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_link_account, savedInstanceState);
        initViews();
        return view;
    }

    void initViews() {
        showToolbar();
        setTitle("LinkAccount");
        Bundle args = getArguments();
        if (args != null) {
            String suggestedName = args.getString(ARG_SUGGESTED_USER_NAME);
            if (!TextUtils.isEmpty(suggestedName)) {
                mUserName.setText(suggestedName);
            }
        }
        ImageLoader.getInstance().displayImage(SessionManager.getInstance().getAvatarUrl(),
                mAvatar, ImageUtils.getAvatarOptions());
    }

    @OnClick(R.id.btn_next)
    void linkAccount() {
        JSONObject body = new JSONObject();
        final String userName = mUserName.getText().toString();
        try {
            body.put(JsonKey.USERNAME, userName);
            body.put(JsonKey.PASSWORD, mPassword.getText());
        } catch (JSONException e) {
            Logger.t(TAG).e(e, "");
        }

        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.POST, Constants.API_LINK_ACCOUNT, body,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.e("test", "Response: " + response);
                                boolean result = response.optBoolean("result");
                                if (result) {
                                    SessionManager.getInstance().updateLinkStatus(userName, true);
                                    getActivity().finish();
                                } else {
                                    mViewAnimator.setDisplayedChild(0);
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                        showMessage(errorInfo.msgResID);
                        if (errorInfo.errorCode == ServerMessage.USER_NAME_HAS_BEEN_USED) {
                            mUserName.requestFocus();
                        }
                        mViewAnimator.setDisplayedChild(0);
                    }
                })
        );
        mRequestQueue.start();
        mViewAnimator.setDisplayedChild(1);
    }


}
