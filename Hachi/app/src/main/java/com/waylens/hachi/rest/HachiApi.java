package com.waylens.hachi.rest;

import com.waylens.hachi.rest.body.CreateMomentBody;
import com.waylens.hachi.rest.body.DeviceLoginBody;
import com.waylens.hachi.rest.body.FollowPostBody;
import com.waylens.hachi.rest.body.LikePostBody;
import com.waylens.hachi.rest.body.ReportBody;
import com.waylens.hachi.rest.body.SignInPostBody;
import com.waylens.hachi.rest.body.SignUpPostBody;
import com.waylens.hachi.rest.response.CloudStorageInfo;
import com.waylens.hachi.rest.response.CreateMomentResponse;
import com.waylens.hachi.rest.response.FollowInfo;
import com.waylens.hachi.rest.response.LikeResponse;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.rest.response.MomentInfo;
import com.waylens.hachi.rest.response.MomentPlayInfo;
import com.waylens.hachi.rest.response.RawDataResponse;
import com.waylens.hachi.rest.response.SignUpResponse;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.rest.response.SignInResponse;
import com.waylens.hachi.ui.entities.UserProfile;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
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

    @GET("/api/users/me")
    Call<UserInfo> getMyUserInfo();

    @GET("/api/share/accounts")
    Call<LinkedAccounts> getLinkedAccounts();

    @POST("/api/reports")
    Call<SimpleBoolResponse> report(@Body ReportBody reportBody);

    @POST("/api/users/signin")
    Call<SignInResponse> signin(@Body SignInPostBody signInPostBody);

    @POST("/api/users/signup")
    Call<SignUpResponse> signUp(@Body SignUpPostBody signUpnPostBody);

    @GET("/api/users/check_account")
    Call<SimpleBoolResponse> checkEmail(@Query("e") String email);

    @POST("/api/devices/login")
    Call<SignInResponse> deviceLogin(@Body DeviceLoginBody deviceLoginBody);


    @GET("{url}")
    Call<RawDataResponse> getRawData(@Path("url") String url);


    @DELETE("/api/moments/{momentId}")
    Call<SimpleBoolResponse> deleteMoment(@Path("momentId") long momentId);

    @POST("/api/moments")
    Call<CreateMomentResponse> createMoment(@Body CreateMomentBody createMomentBody);

    @GET("/api/cloud/usage")
    Call<CloudStorageInfo> getCloudStorageInfo();

}