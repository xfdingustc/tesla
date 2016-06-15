package com.rest;

import com.rest.body.FollowPostBody;
import com.rest.body.LikePostBody;
import com.rest.body.ReportBody;
import com.rest.response.FollowInfo;
import com.rest.response.LikeResponse;
import com.rest.response.LinkedAccounts;
import com.rest.response.MomentInfo;
import com.rest.response.MomentPlayInfo;
import com.rest.response.SimpleBoolResponse;
import com.rest.response.UserInfo;
import com.waylens.hachi.ui.entities.UserProfile;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;

/**
 * Created by Xiaofei on 2016/6/8.
 */
public interface HachiApi {
    @GET("/api/users/{userId}")
    Call<UserProfile> getUserProfile(@Path("userId") String userId);

    @GET("/api/moments/play/{momentId}")
    Call<MomentPlayInfo> getRawDataUrl(@Path("momentId") long momentId);

    @GET("/api/moments/play/{momentId}")
    Observable<MomentPlayInfo> getMomentPlayInfo(@Path("momentId") long momentId);

    @POST("/api/likes")
    Call<LikeResponse> like(@Body LikePostBody likePostBody);

    @POST("/api/friends/follow")
    Call<SimpleBoolResponse> follow(@Body FollowPostBody followPostBody);

    @POST("/api/friends/unfollow")
    Call<SimpleBoolResponse> unfollow(@Body FollowPostBody followPostBody);

    @GET("/api/moments/{momentId}")
    Call<MomentInfo> getMomentInfo(@Path("momentId") long momentId);

    @GET("/api/moments/{momentId}")
    Observable<MomentInfo> getMomentInfoRx(@Path("momentId") long momentId);

    @GET("/api/friends/{userId}")
    Call<FollowInfo> getFollowInfo(@Path("userId") String userId);

    @GET("/api/users/{userId}")
    Call<UserInfo> getUserInfo(@Path("userId") String userId);

    @GET("/api/share/accounts")
    Call<LinkedAccounts> getLinkedAccounts();

    @POST("/api/reports")
    Call<SimpleBoolResponse> report(@Body ReportBody reportBody);
}
