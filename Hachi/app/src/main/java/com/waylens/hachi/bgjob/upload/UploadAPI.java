package com.waylens.hachi.bgjob.upload;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.service.upload.UploadService;
import com.waylens.hachi.service.upload.rest.body.InitUploadBody;
import com.waylens.hachi.service.upload.rest.response.FinishUploadResponse;
import com.waylens.hachi.service.upload.rest.response.UploadDataResponse;
import com.waylens.hachi.service.upload.rest.response.InitUploadResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.entities.LocalMoment;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Xiaofei on 2016/9/7.
 */
public class UploadAPI {
    private static final String TAG = UploadAPI.class.getSimpleName();
    private static final int DEFAULT_TIMEOUT = 15;
    private Retrofit mRetrofit;


    public UploadAPI(String baseUrl, final String date, final String authorization) {


        TrustManager[] trustManager = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    X509Certificate[] cArrr = new X509Certificate[0];
                    return cArrr;
                }
            }
        };
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustManager, new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();


            OkHttpClient client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        Request newReq = request.newBuilder()
                            .addHeader("Date", date)
                            .addHeader("Authorization", authorization)
                            .build();

                        return chain.proceed(newReq);
                    }
                })
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession sslSession) {
                        return true;
                    }
                })
                .build();

            mRetrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }


    }


    public InitUploadResponse initUploadSync(long momentId, InitUploadBody body) {
        try {
            Call<InitUploadResponse> initUploadResponseCall = mRetrofit.create(UploadService.class)
                .initUpload(SessionManager.getInstance().getUserId(), "init", momentId, body);
            Logger.t(TAG).d("url: " + initUploadResponseCall.request().url().toString());
            return initUploadResponseCall.execute().body();


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public UploadDataResponse uploadChunkSync(RequestBody requestBody, long momentId, LocalMoment.Segment segment) {
        try {
            Call<UploadDataResponse> uploadDataResponseCall = mRetrofit.create(UploadService.class)
                .uploadData(SessionManager.getInstance().getUserId(),
                    "transfer",
                    momentId,
                    segment.dataType,
                    segment.clip.getVdbId(),
                    segment.uploadURL.realTimeMs,
                    0,
                    segment.uploadURL.lengthMs,
                    requestBody);
            return uploadDataResponseCall.execute().body();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public UploadDataResponse uploadThumbnail(RequestBody requestBody, long momentId) {
        try {
            Call<UploadDataResponse> uploadDataResponseCall = mRetrofit.create(UploadService.class)
                .uploadData(SessionManager.getInstance().getUserId(),
                    "transfer",
                    momentId,
                    256,
                    requestBody);
            return uploadDataResponseCall.execute().body();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public void finishUpload(long momentId) {
        try {
            Call<Void> finishUploadResponseCall = mRetrofit.create(UploadService.class)
                .finishUpload(SessionManager.getInstance().getUserId(),
                    "finish",
                    momentId
                    );
            finishUploadResponseCall.execute().body();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
