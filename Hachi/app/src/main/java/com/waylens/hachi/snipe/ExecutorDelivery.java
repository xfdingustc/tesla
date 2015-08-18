package com.waylens.hachi.snipe;


import android.os.Handler;

import java.util.concurrent.Executor;

/**
 * Created by Xiaofei on 2015/8/17.
 */
public class ExecutorDelivery implements ResponseDelivery {


    private final Executor mResponsePoster;

    public ExecutorDelivery(final Handler handler) {
        mResponsePoster = new Executor() {
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        };

    }

    @Override
    public void postResponse(VdbRequest<?> vdbRequest, VdbResponse<?> vdbResponse) {
        postResponse(vdbRequest, vdbResponse, null);
    }

    @Override
    public void postResponse(VdbRequest<?> vdbRequest, VdbResponse<?> vdbResponse, Runnable runnable) {

    }

    @Override
    public void postError(VdbRequest<?> vdbRequest, SnipeError error) {

    }
}
