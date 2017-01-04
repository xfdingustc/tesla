package com.waylens.hachi.service.upload;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.service.upload.rest.body.InitUploadBody;
import com.waylens.hachi.service.upload.rest.response.InitUploadResponse;
import com.waylens.hachi.service.upload.rest.response.UploadDataResponse;
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
        this(baseUrl, date, authorization, DEFAULT_TIMEOUT);
    }


    public UploadAPI(String baseUrl, final String date, final String authorization, int timeOut) {


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


            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
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

                .sslSocketFactory(sslSocketFactory)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession sslSession) {
                        return true;
                    }
                });

            if (timeOut > 0) {
                clientBuilder.connectTimeout(timeOut, TimeUnit.SECONDS);
            }
            clientBuilder.readTimeout(0, TimeUnit.SECONDS);
            clientBuilder.writeTimeout(0, TimeUnit.SECONDS);
            clientBuilder.connectTimeout(0, TimeUnit.SECONDS);

            OkHttpClient client = clientBuilder.build();

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


    public InitUploadResponse initUploadSync(long momentId, InitUploadBody body) throws IOException {
        Call<InitUploadResponse> initUploadResponseCall = mRetrofit.create(IUploadService.class)
            .initUpload(SessionManager.getInstance().getUserId(), "init", momentId, body);
        Logger.t(TAG).d("url: " + initUploadResponseCall.request().url().toString());
        return initUploadResponseCall.execute().body();
    }

    public UploadDataResponse uploadChunkSync(RequestBody requestBody, long momentId, LocalMoment.Segment segment) throws IOException {
        Call<UploadDataResponse> uploadDataResponseCall = mRetrofit.create(IUploadService.class)
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
    }


    public UploadDataResponse uploadThumbnail(RequestBody requestBody, long momentId) throws IOException {
        Call<UploadDataResponse> uploadDataResponseCall = mRetrofit.create(IUploadService.class)
            .uploadData(SessionManager.getInstance().getUserId(),
                "transfer",
                momentId,
                256,
                requestBody);
        return uploadDataResponseCall.execute().body();
    }


    public void finishUpload(long momentId) throws IOException {
        Call<Void> finishUploadResponseCall = mRetrofit.create(IUploadService.class)
            .finishUpload(SessionManager.getInstance().getUserId(),
                "finish",
                momentId
            );
        finishUploadResponseCall.execute().body();
    }


    public UploadDataResponse uploadAvatarSync(RequestBody requestBody, String sha1) {
        try {
            Call<UploadDataResponse> uploadAvatarCall = mRetrofit.create(IUploadService.class)
                .uploadAvatar(SessionManager.getInstance().getUserId(), sha1, requestBody);
            return uploadAvatarCall.execute().body();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public UploadDataResponse uploadPictureSync(RequestBody requestBody, long momentId, String sha1) {

        try {
            Call<UploadDataResponse> uploadPictureCall = mRetrofit.create(IUploadService.class)
                .uploadPicture(SessionManager.getInstance().getUserId(), momentId, sha1, "public", requestBody);
            return uploadPictureCall.execute().body();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public UploadDataResponse uploadMp4Sync(RequestBody requestBody, long momentId, String sha1, long resolution, long duration) {
        try {
            Call<UploadDataResponse> uploadMp4Call = mRetrofit.create(IUploadService.class)
                    .uploadMp4(SessionManager.getInstance().getUserId(), momentId, sha1, "public", resolution, duration, requestBody);
            return uploadMp4Call.execute().body();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
