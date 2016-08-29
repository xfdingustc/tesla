package com.waylens.hachi.service.upload;

import com.waylens.hachi.service.upload.rest.body.InitUploadBody;
import com.waylens.hachi.service.upload.rest.response.UploadDataResponse;
import com.waylens.hachi.service.upload.rest.response.InitUploadResponse;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by Xiaofei on 2016/9/7.
 */
public interface UploadService {
    @PUT("/v.1.0/upload_videos/{userId}/android")
    Call<InitUploadResponse> initUpload(@Path("userId") String userId,
                                        @Query("upload_phase") String uploadPhase,
                                        @Query("moment_id") long momentId,
                                        @Body InitUploadBody uploadBody);

    @Headers("Transfer-Encoding: chunked")
    @PUT("/v.1.0/upload_videos/{userId}/android")
    Call<UploadDataResponse> uploadData(@Path("userId") String userId,
                                        @Query("upload_phase") String uploadPhase,
                                        @Query("moment_id") long momentId,
                                        @Query("data_type") int dataType,
                                        @Query("guid") String guid,
                                        @Query("begin_time") long beginTime,
                                        @Query("offset") int offset,
                                        @Query("duration") int duration,
                                        @Body RequestBody requestBody);


    @Headers("Transfer-Encoding: chunked")
    @PUT("/v.1.0/upload_videos/{userId}/android")
    Call<UploadDataResponse> uploadData(@Path("userId") String userId,
                                        @Query("upload_phase") String uploadPhase,
                                        @Query("moment_id") long momentId,
                                        @Query("data_type") int dataType,
                                        @Body RequestBody requestBody);

    @PUT("/v.1.0/upload_videos/{userId}/android")
    Call<Void> finishUpload(@Path("userId") String userId,
                                            @Query("upload_phase") String uploadPhase,
                                            @Query("moment_id") long momentId);
}