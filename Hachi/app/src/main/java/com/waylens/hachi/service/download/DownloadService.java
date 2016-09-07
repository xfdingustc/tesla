package com.waylens.hachi.service.download;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;
import rx.Observable;

/**
 * Created by Xiaofei on 2016/9/1.
 */
public interface DownloadService {
    @Streaming
    @GET
    Observable<ResponseBody> download(@Url String url);

    @Streaming
    @GET
    Call<ResponseBody> downloadSync(@Url String url);
}
