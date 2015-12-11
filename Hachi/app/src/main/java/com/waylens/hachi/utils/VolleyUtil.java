package com.waylens.hachi.utils;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by Richard on 12/11/15.
 */
public class VolleyUtil {
    private static volatile RequestQueue _VOLLEY_REQUEST_QUEUE;

    public static RequestQueue newVolleyRequestQueue(Context context) {
        if (_VOLLEY_REQUEST_QUEUE == null) {
            synchronized (VolleyUtil.class) {
                if (_VOLLEY_REQUEST_QUEUE == null) {
                    _VOLLEY_REQUEST_QUEUE = Volley.newRequestQueue(context);
                    _VOLLEY_REQUEST_QUEUE.start();
                }
            }
        }
        return _VOLLEY_REQUEST_QUEUE;
    }

}
