package com.rest;

import android.text.TextUtils;

import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;

import java.io.IOException;

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


    private HachiService() {

    }

    public static HachiApi createHachiApiService() {
        Retrofit.Builder builder = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("http://ws.waylens.com:9000");

        final String token = SessionManager.getInstance().getToken();
        if (!TextUtils.isEmpty(token)) {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();
                    Request newReq = request.newBuilder()
                        .addHeader("X-Auth-Token", token)
                        .build();
                    return chain.proceed(newReq);
                }
            }).build();

            builder.client(client);
        }

        return builder.build().create(HachiApi.class);
    }
}
