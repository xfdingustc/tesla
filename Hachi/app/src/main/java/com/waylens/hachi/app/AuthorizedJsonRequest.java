package com.waylens.hachi.app;

import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.waylens.hachi.session.SessionManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * JsonObjectRequest with token
 * Created by Richard on 8/18/15.
 */
public class AuthorizedJsonRequest extends JsonObjectRequest {
    private HashMap<String, String> mHashMap = new HashMap<>();

    private String mToken;

    public AuthorizedJsonRequest(int method,
                                 String url,
                                 Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener, String token) {
        super(method, url, listener, errorListener);
        mToken = token;
    }

    public AuthorizedJsonRequest(int method,
                                 String url,
                                 Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
        mToken = null;
    }

    public AuthorizedJsonRequest(int method,
                                 String url,
                                 JSONObject jsonRequest,
                                 Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
        mToken = null;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        if (TextUtils.isEmpty(mToken)) {
            mToken = SessionManager.getInstance().getToken();
        }

        if (!TextUtils.isEmpty(mToken)) {
            mHashMap.put("X-Auth-Token", mToken);
        }
        return mHashMap;
    }
}
