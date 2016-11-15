package com.waylens.hachi.rest;

import android.os.Build;
import android.text.TextUtils;

import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Xiaofei on 2016/6/8.
 */
public class HachiService {
    private static final String TAG = HachiService.class.getSimpleName();
    public static IHachiApi mHachiApiInstance = null;
    public static final int TIME_OUT_MILLI_SEC = 10000;

    private static String USER_AGENT = "Android " + Build.VERSION.SDK + ";" + Build.BRAND + Build.MODEL;

    private HachiService() {

    }

    public static IHachiApi createHachiApiService() {
        if (mHachiApiInstance == null) {
            synchronized (HachiService.class) {
                if (mHachiApiInstance == null) {
                    Retrofit.Builder builder = new Retrofit.Builder().addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .baseUrl(Constants.HOST_URL);
                    OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

                    clientBuilder.addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request request = chain.request();
                            Request.Builder newReqBuilder = request.newBuilder()
                                .addHeader("User-Agent", USER_AGENT);


                            final String token = SessionManager.getInstance().getToken();
                            if (!TextUtils.isEmpty(token)) {
                                newReqBuilder.addHeader("X-Auth-Token", token);
                            }

                            return chain.proceed(newReqBuilder.build());
                        }
                    })
                        .readTimeout(TIME_OUT_MILLI_SEC, TimeUnit.MILLISECONDS)
                        .connectTimeout(TIME_OUT_MILLI_SEC, TimeUnit.MILLISECONDS);

                    builder.client(clientBuilder.build());

                    mHachiApiInstance = builder.build().create(IHachiApi.class);
                }
            }
        }

        return mHachiApiInstance;


    }


    public static IHachiApi createHachiApiService(int timeout, TimeUnit timeUnit) {
        Retrofit.Builder builder = new Retrofit.Builder().addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(Constants.HOST_URL);

        final String token = SessionManager.getInstance().getToken();
        if (!TextUtils.isEmpty(token)) {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();
                    Request newReq = request.newBuilder()
                        .addHeader("X-Auth-Token", token)
                        .addHeader("User-Agent", USER_AGENT)
                        .build();
                    return chain.proceed(newReq);
                }
            })
                .readTimeout(timeout, timeUnit)
                .connectTimeout(timeout, timeUnit)
                .build();


            builder.client(client);
        }

        return builder.build().create(IHachiApi.class);
    }


}
