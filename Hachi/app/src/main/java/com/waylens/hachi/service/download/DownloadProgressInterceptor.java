package com.waylens.hachi.service.download;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Created by Xiaofei on 2016/9/1.
 */
public class DownloadProgressInterceptor implements Interceptor {

    private final DownloadProgressListener mListener;

    public DownloadProgressInterceptor(DownloadProgressListener listener) {
        this.mListener = listener;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());
        return originalResponse.newBuilder()
            .body(new DownloadProgressResponseBody(originalResponse.body(), mListener))
            .build();
    }
}
