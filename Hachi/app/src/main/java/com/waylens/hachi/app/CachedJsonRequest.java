package com.waylens.hachi.app;

import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.waylens.hachi.session.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * JsonObjectRequest with token and cache
 *
 */
public class CachedJsonRequest extends JsonObjectRequest {
    private HashMap<String, String> mHashMap = new HashMap<>();

    private String mToken;

    public CachedJsonRequest(int method, String url, Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener, String token) {
        super(method, url, listener, errorListener);
        mToken = token;
        setTimeout();
    }

    public CachedJsonRequest(int method, String url, Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
        mToken = null;
        setTimeout();
    }

    public CachedJsonRequest(int method, String url, String requestBody, Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, requestBody, listener, errorListener);
        mToken = null;
        setTimeout();
    }

    public CachedJsonRequest(String url, Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
        super(url, listener, errorListener);
        mToken = null;
        setTimeout();
    }

    public CachedJsonRequest(int method, String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener,
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

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
        if (cacheEntry == null) {
            cacheEntry = new Cache.Entry();
        }
        final long cacheHitButRefreshed = (long) 100 * 24 * 60 * 60 * 1000;
        final long cacheExpired = (long) 100 * 24 * 60 * 60 * 1000; // in 100 days this cache entry expires completely
        long now = System.currentTimeMillis();
        final long softExpire = now + cacheHitButRefreshed;
        final long ttl = now + cacheExpired;
        cacheEntry.data = response.data;
        cacheEntry.softTtl = softExpire;
        cacheEntry.ttl = ttl;
        String headerValue;
        headerValue = response.headers.get("Date");
        if (headerValue != null) {
            cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
        }
        headerValue = response.headers.get("Last-Modified");
        if (headerValue != null) {
            cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);
        }
        cacheEntry.responseHeaders = response.headers;

        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            return Response.success(new JSONObject(jsonString),
                    cacheEntry);
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
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

        public Builder delete() {
            mMethod = Method.DELETE;
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

        public CachedJsonRequest build() {
            if (mMethod == Method.GET) {
                return new CachedJsonRequest(mMethod, mUrl, mListener, mErrorListener);
            } else {
                return new CachedJsonRequest(mMethod, mUrl, mPostBody, mListener, mErrorListener);
            }
        }
    }
}

