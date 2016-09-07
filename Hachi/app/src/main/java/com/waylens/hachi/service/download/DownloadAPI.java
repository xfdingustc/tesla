package com.waylens.hachi.service.download;

import android.support.annotation.NonNull;

import com.waylens.hachi.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/9/1.
 */
public class DownloadAPI {

    private static final int DEFAULT_TIMEOUT = 15;
    public Retrofit mRetrofit;

    public DownloadAPI(String url, DownloadProgressListener listener) {
        DownloadProgressInterceptor interceptor = new DownloadProgressInterceptor(listener);

        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .retryOnConnectionFailure(true)
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build();

        mRetrofit = new Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .build();
    }


    public void downloadFile(@NonNull String url, final File file, Subscriber subscriber) {
        mRetrofit.create(DownloadService.class)
            .download(url)
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .map(new Func1<ResponseBody, InputStream>() {
                @Override
                public InputStream call(ResponseBody responseBody) {
                    return responseBody.byteStream();
                }
            })
            .observeOn(Schedulers.computation())
            .doOnNext(new Action1<InputStream>() {
                @Override
                public void call(InputStream inputStream) {
                    try {
                        FileUtils.writeFile(inputStream, file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(subscriber);
    }


    public InputStream downloadFileSync(@NonNull String url) throws IOException {
        ResponseBody responseBody = mRetrofit.create(DownloadService.class)
            .downloadSync(url).execute().body();
        if (responseBody != null) {
            return responseBody.byteStream();
        } else {
            return null;
        }
    }
}
