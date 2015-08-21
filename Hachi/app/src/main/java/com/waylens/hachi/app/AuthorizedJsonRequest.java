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

    public AuthorizedJsonRequest(int method,
                                 String url,
                                 Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
    }

    public AuthorizedJsonRequest(int method,
                                 String url,
                                 JSONObject jsonRequest,
                                 Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        String token = SessionManager.getInstance().getToken();
        if (!TextUtils.isEmpty(token)) {
            mHashMap.put("X-Auth-Token", token);
        }
        return mHashMap;
    }
}
