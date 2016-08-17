package com.waylens.hachi.rxjava;


import rx.Subscriber;

/**
 * Created by Xiaofei on 2016/8/18.
 */
public abstract class SimpleSubscribe<T> extends Subscriber<T> {

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
    }
}
