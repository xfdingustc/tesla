package com.waylens.hachi.app;

import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
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
        setTimeout();
    }

    public AuthorizedJsonRequest(int method,
                                 String url,
                                 Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
        mToken = null;
        setTimeout();
    }

    public AuthorizedJsonRequest(int method,
                                 String url,
                                 String requestBody,
                                 Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, requestBody, listener, errorListener);
        mToken = null;
        setTimeout();
    }

    public AuthorizedJsonRequest(String url,
                                 Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(url, listener, errorListener);
        mToken = null;
        setTimeout();
    }

    public AuthorizedJsonRequest(int method,
                                 String url,
                                 JSONObject jsonRequest,
                                 Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
        mToken = null;
        setTimeout();
    }

    private void setTimeout() {
        setRetryPolicy(
                new DefaultRetryPolicy(
                        1000 * 10,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
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
