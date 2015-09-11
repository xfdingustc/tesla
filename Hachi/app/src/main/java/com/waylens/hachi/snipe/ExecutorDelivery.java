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
        vdbRequest.markDelivered();
        vdbRequest.addMarker("post-response");
        mResponsePoster.execute(new ResponseDeliveryRunnable(vdbRequest, vdbResponse, runnable));

    }

    @Override
    public void postError(VdbRequest<?> vdbRequest, SnipeError error) {

    }

    private class ResponseDeliveryRunnable implements Runnable {

        private final VdbRequest mRequest;
        private final VdbResponse mResponse;
        private final Runnable mRunnable;

        public ResponseDeliveryRunnable(VdbRequest vdbRequest, VdbResponse vdbResponse, Runnable
            runnable) {
            mRequest = vdbRequest;
            mResponse = vdbResponse;
            mRunnable = runnable;
        }

        @Override
        public void run() {
            if (mRequest.isCanceled()) {
                mRequest.finish("canceled-at-delivery");
                return;
            }


            if (mResponse != null && mResponse.isSuccess()) {
                mRequest.deliverResponse(mResponse.result);
            }
        }
    }
}
