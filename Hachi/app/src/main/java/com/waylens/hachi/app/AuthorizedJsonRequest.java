package com.waylens.hachi.app;

import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.cocosw.bottomsheet.BottomSheet;
import com.waylens.hachi.session.SessionManager;

import org.json.JSONException;
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

    public AuthorizedJsonRequest(int method, String url, Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener, String token) {
        super(method, url, listener, errorListener);
        mToken = token;
        setTimeout();
    }

    public AuthorizedJsonRequest(int method, String url, Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
        mToken = null;
        setTimeout();
    }

    public AuthorizedJsonRequest(int method, String url, String requestBody, Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, requestBody, listener, errorListener);
        mToken = null;
        setTimeout();
    }

    public AuthorizedJsonRequest(String url, Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(url, listener, errorListener);
        mToken = null;
        setTimeout();
    }

    public AuthorizedJsonRequest(int method, String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener,
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

    public static class Builder {
        private int mMethod;
        private String mUrl;
        private Response.Listener<JSONObject> mListener;
        private Response.ErrorListener mErrorListener;
        private JSONObject mPostBody;

        public Builder() {
            mMethod = Method.GET;
            mListener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                }
            };
            mErrorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            };
            mPostBody = new JSONObject();
        }

        public Builder method(int method) {
            mMethod = method;
            return this;
        }

        public Builder url(String url) {
            mUrl = url;
            return this;
        }

        public Builder postBody(String key, String value) {
            mMethod = Method.POST;
            try {
                mPostBody.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return this;
        }

        public Builder postBody(String key, long value) {
            mMethod = Method.POST;
            try {
                mPostBody.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return this;
        }

        public Builder listner(Response.Listener<JSONObject> listener) {
            mListener = listener;
            return this;
        }

        public Builder errorListener(Response.ErrorListener errorListener) {
            mErrorListener = errorListener;
            return this;
        }

        public AuthorizedJsonRequest build() {
            if (mMethod == Method.GET) {
                return new AuthorizedJsonRequest(mMethod, mUrl, mListener, mErrorListener);
            } else {
                return new AuthorizedJsonRequest(mMethod, mUrl, mPostBody, mListener, mErrorListener);
            }
        }
    }


}
