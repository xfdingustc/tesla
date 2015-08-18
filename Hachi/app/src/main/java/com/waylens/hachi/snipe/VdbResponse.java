package com.waylens.hachi.snipe;

/**
 * Created by Xiaofei on 2015/8/17.
 */
public class VdbResponse<T> {
    public interface Listener<T> {
        void onResponse(T response);
    }

    public interface ErrorListener {
        void onErrorResponse(SnipeError error);
    }
}
