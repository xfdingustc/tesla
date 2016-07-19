package com.waylens.hachi.listeners;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public interface BaseMultiLoadedListener<T> {
    void onSuccess(int eventTag, T data);

    void onError(String msg);


    void onException(String msg);
}
