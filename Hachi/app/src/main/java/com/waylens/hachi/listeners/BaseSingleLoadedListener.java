package com.waylens.hachi.listeners;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public interface BaseSingleLoadedListener<T> {
    void onSuccess(T data);

    void onError(String msg);

    void onException(String msg);
}
