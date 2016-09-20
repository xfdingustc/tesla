package com.waylens.hachi.rest;

import com.waylens.hachi.rest.body.CreateMomentBody;
import com.waylens.hachi.rest.body.FollowPostBody;
import com.waylens.hachi.rest.body.LikePostBody;
import com.waylens.hachi.rest.body.MomentUpdateBody;
import com.waylens.hachi.rest.body.ReportCommentBody;
import com.waylens.hachi.rest.body.ReportMomentBody;
import com.waylens.hachi.rest.body.ReportUserBody;
import com.waylens.hachi.rest.body.RepostBody;
import com.waylens.hachi.rest.body.SignInPostBody;
import com.waylens.hachi.rest.body.SignUpPostBody;
import com.waylens.hachi.rest.body.SocialProvider;
import com.waylens.hachi.rest.response.MakerResponse;
import com.waylens.hachi.rest.response.ModelResponse;
import com.waylens.hachi.rest.response.VinQueryResponse;
import com.waylens.hachi.rest.response.CloudStorageInfo;
import com.waylens.hachi.rest.response.CreateMomentResponse;
import com.waylens.hachi.rest.response.DeleteCommentResponse;
import com.waylens.hachi.rest.response.FollowInfo;
import com.waylens.hachi.rest.response.FriendList;
import com.waylens.hachi.rest.response.GeoInfoResponse;
import com.waylens.hachi.rest.response.LikeResponse;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.rest.response.MomentInfo;
import com.waylens.hachi.rest.response.MomentListResponse;
import com.waylens.hachi.rest.response.MomentListResponse2;
import com.waylens.hachi.rest.response.MomentPlayInfo;
import com.waylens.hachi.rest.response.MusicCategoryResponse;
import com.waylens.hachi.rest.response.MusicList;
import com.waylens.hachi.rest.response.RaceQueryResponse;
import com.waylens.hachi.rest.response.RepostResponse;
import com.waylens.hachi.rest.response.SignUpResponse;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.rest.response.UploadAvatarResponse;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.rest.response.SignInResponse;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by Xiaofei on 2016/6/8.
 */
public interface HachiApi {
//    @GET("/api/users/{userId}")
//    Call<UserProfile> getUserProfile(@Path("userId") String userId);

//    @GET("/api/moments/play/{momentId}")
//    Call<MomentPlayInfo> getRawDataUrl(@Path("momentId") long momentId);

    @GET("/api/users/start_upload_avatar")
    Call<UploadAvatarResponse> getAvatarUploadServer();

    @GET("/api/moments/play/{momentId}")
    Observable<MomentPlayInfo> getMomentPlayInfo(@Path("momentId") long momentId);

    @POST("/api/likes")
    Call<LikeResponse> like(@Body LikePostBody likePostBody);

    @POST("/api/friends/follow")
    Call<SimpleBoolResponse> follow(@Body FollowPostBody followPostBody);

    @POST("/api/friends/unfollow")
    Call<SimpleBoolResponse> unfollow(@Body FollowPostBody followPostBody);

    @GET("/api/moments/{momentId}")
    Observable<Response<MomentInfo>> getMomentInfo(@Path("momentId") long momentId);

    @GET("/api/users/{userId}/moments")
    Call<MomentListResponse> getUserMoments(@Path("userId") String userId, @Query("cursor") int cursor);

    @GET("/api/users/{userId}/moments")
    Observable<MomentListResponse> getUserMomentsRx(@Path("userId") String userId, @Query("cursor") int cursor);

    @GET("/api/moments/recommend")
    Observable<MomentListResponse2> getRecommendedMomentsRx(@Query("count") int count);

    @GET("/api/moments/myfeed")
    Observable<MomentListResponse2> getMyFeed(@Query("cursor") long cursor, @Query("count") int count, @Query("order") String order);


    @GET("/api/moments/{momentId}")
    Observable<MomentInfo> getMomentInfoRx(@Path("momentId") long momentId);

    @GET("/api/friends/{userId}")
    Call<FollowInfo> getFollowInfo(@Path("userId") String userId);

    @GET("/api/friends/{userId}")
    Observable<FollowInfo> getFollowInfoRx(@Path("userId") String userId);

    @GET("/api/users/{userId}")
    Call<UserInfo> getUserInfo(@Path("userId") String userId);

    @GET("/api/users/{userId}")
    Observable<UserInfo> getUserInfoRx(@Path("userId") String userId);

    @GET("/api/users/me")
    Call<UserInfo> getMyUserInfo();

    @GET("/api/share/accounts")
    Call<LinkedAccounts> getLinkedAccounts();

    @POST("/api/reports")
    Call<SimpleBoolResponse> report(@Body ReportMomentBody reportBody);

    @POST("/api/reports")
    Call<SimpleBoolResponse> report(@Body ReportCommentBody reportBody);

    @POST("/api/reports")
    Call<SimpleBoolResponse> report(@Body ReportUserBody reportBody);

    @POST("/api/users/signin")
    Call<SignInResponse> signin(@Body SignInPostBody signInPostBody);

    @POST("/api/users/signup")
    Call<SignUpResponse> signUp(@Body SignUpPostBody signUpnPostBody);

    @GET("/api/users/check_account")
    Call<SimpleBoolResponse> checkEmail(@Query("e") String email);

//    @POST("/api/devices/login")
//    Call<SignInResponse> deviceLogin(@Body DeviceLoginBody deviceLoginBody);

    @GET("/api/friends/{follow}")
    Observable<FriendList> getFriendListRx(@Path("follow") String follow, @Query("u") String userId);


    @DELETE("/api/moments/{momentId}")
    Call<SimpleBoolResponse> deleteMoment(@Path("momentId") long momentId);

    @POST("/api/moments")
    Call<CreateMomentResponse> createMoment(@Body CreateMomentBody createMomentBody);

    @FormUrlEncoded
    @POST("/api/moments/picture/finish")
    Call<SimpleBoolResponse> finishUploadPictureMoment(@Field("momentID") long momentId, @Field("pictureNum") int pictureNum);

    @GET("/api/cloud/usage")
    Call<CloudStorageInfo> getCloudStorageInfo();

    @DELETE("/api/comments/{commentID}")
    Call<DeleteCommentResponse> deleteComment(@Path("commentID") long commentID);

    @DELETE("/api/share/accounts/{provider}")
    Call<SimpleBoolResponse> unbindSocialProvider(@Path("provider") String provider);

    @POST("/api/share/accounts")
    Call<SimpleBoolResponse> bindSocialProvider(@Body SocialProvider provider);

    @GET("/api/users/resend_verify_mail")
    Call<SimpleBoolResponse> resendVerifyEmail();

    @POST("/api/share/moments")
    Call<RepostResponse> repost(@Body RepostBody repostBody);

    @GET("/api/devices/logout")
    Call<SimpleBoolResponse> deviceLogout();

    @POST("/api/moments/update/{momentId}")
    Call<SimpleBoolResponse> updateMoment(@Path("momentId") long momentId, @Body MomentUpdateBody momentUpdatebody);

    @GET("/api/musics/categories")
    Call<MusicCategoryResponse> getMusicCategories();

    @GET("/api/musics/categories/{categoryId}")
    Observable<MusicList> getMusicList(@Path("categoryId") long categoryId);

    @GET("/api/vehicle")
    Call<VinQueryResponse> queryByVin(@Query("vin") String vin);

    @GET("/api/moments/race")
    Call<RaceQueryResponse>  queryRace(@Query("mode") int mode, @Query("start") int start, @Query("end") int end, @Query("maker") String maker, @Query("model") String model, @Query("count") int count);

    @GET("/api/place")
    Call<GeoInfoResponse> getGeoInfo(@Query("lon") double lon, @Query("lat") double lat);

    @GET("api/vehicle/makers")
    Call<MakerResponse> getAllMaker();

    @GET("api/vehicle/models")
    Call<ModelResponse> getModelByMaker(@Query("maker") long makerID);

}
