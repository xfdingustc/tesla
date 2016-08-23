package com.waylens.hachi.rest;

import android.os.Build;
import android.text.TextUtils;

import com.orhanobut.logger.Logger;
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

    public static HachiApi mHachiApiInstance = null;

    private static String USER_AGENT = "Android " + Build.VERSION.SDK + ";" + Build.BRAND + Build.MODEL;
    private HachiService() {

    }

    public static HachiApi createHachiApiService() {
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
            }).build();

            builder.client(client);
        }

        mHachiApiInstance = builder.build().create(HachiApi.class);
        return mHachiApiInstance;
    }


}
