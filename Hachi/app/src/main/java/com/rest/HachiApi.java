package com.rest;

import com.rest.body.LikePostBody;
import com.rest.response.LikeResponse;
import com.waylens.hachi.ui.entities.User;
import com.waylens.hachi.ui.entities.UserProfile;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Created by Xiaofei on 2016/6/8.
 */
public interface HachiApi {
    @GET("/api/users/{userId}")
    Call<UserProfile> getUserInfo(@Path("userId") String userId);

    @POST("/api/likes")
    Call<LikeResponse> like(@Body LikePostBody likePostBody);


}
