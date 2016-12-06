package com.waylens.hachi.rest;

import com.waylens.hachi.rest.bean.Comment;
import com.waylens.hachi.rest.bean.Firmware;
import com.waylens.hachi.rest.bean.MomentAmount;
import com.waylens.hachi.rest.body.AddMomentViewCountBody;
import com.waylens.hachi.rest.body.AddVehicleBody;
import com.waylens.hachi.rest.body.ChangePwdBody;
import com.waylens.hachi.rest.body.CreateMomentBody;
import com.waylens.hachi.rest.body.DeviceLoginBody;
import com.waylens.hachi.rest.body.FinishUploadBody;
import com.waylens.hachi.rest.body.FollowPostBody;
import com.waylens.hachi.rest.body.LikePostBody;
import com.waylens.hachi.rest.body.MomentUpdateBody;
import com.waylens.hachi.rest.body.PublishCommentBody;
import com.waylens.hachi.rest.body.ReportCommentBody;
import com.waylens.hachi.rest.body.ReportFeedbackBody;
import com.waylens.hachi.rest.body.ReportMomentBody;
import com.waylens.hachi.rest.body.ReportUserBody;
import com.waylens.hachi.rest.body.RepostBody;
import com.waylens.hachi.rest.body.ResetPwdBody;
import com.waylens.hachi.rest.body.SignInPostBody;
import com.waylens.hachi.rest.body.SignUpPostBody;
import com.waylens.hachi.rest.body.SocialProvider;
import com.waylens.hachi.rest.body.UserProfileBody;
import com.waylens.hachi.rest.body.VehicleListResponse;
import com.waylens.hachi.rest.response.AuthorizeResponse;
import com.waylens.hachi.rest.response.CityList;
import com.waylens.hachi.rest.response.CloudStorageInfo;
import com.waylens.hachi.rest.response.CommentListResponse;
import com.waylens.hachi.rest.response.CountryListResponse;
import com.waylens.hachi.rest.response.CreateMomentResponse;
import com.waylens.hachi.rest.response.DeleteCommentResponse;
import com.waylens.hachi.rest.response.FirmwareResponse;
import com.waylens.hachi.rest.response.FollowInfo;
import com.waylens.hachi.rest.response.FriendList;
import com.waylens.hachi.rest.response.GeoInfoResponse;
import com.waylens.hachi.rest.response.LikeResponse;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.rest.response.MakerResponse;
import com.waylens.hachi.rest.response.ModelResponse;
import com.waylens.hachi.rest.response.ModelYearResponse;
import com.waylens.hachi.rest.response.MomentInfo;
import com.waylens.hachi.rest.response.MomentListResponse;
import com.waylens.hachi.rest.response.MomentPlayInfo;
import com.waylens.hachi.rest.response.MomentSummaryResponse;
import com.waylens.hachi.rest.response.MusicCategoryResponse;
import com.waylens.hachi.rest.response.MusicList;
import com.waylens.hachi.rest.response.NotificationResponse;
import com.waylens.hachi.rest.response.PublishCommentResponse;
import com.waylens.hachi.rest.response.RaceQueryResponse;
import com.waylens.hachi.rest.response.RepostResponse;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.rest.response.UploadAvatarResponse;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.rest.response.VinQueryResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by Xiaofei on 2016/6/8.
 */
public interface IHachiApi {
//    @GET("/api/users/{userId}")
//    Call<UserProfile> getUserProfile(@Path("userId") String userId);

//    @GET("/api/moments/play/{momentId}")
//    Call<MomentPlayInfo> getRawDataUrl(@Path("momentId") long momentId);

    @GET("api/users/send_passwordreset_mail")
    Observable<SimpleBoolResponse> sendPwdResetEmailRx(@Query("e") String email);

    @POST("api/users/reset_password")
    Observable<SimpleBoolResponse> resetPasswordRx(@Body ResetPwdBody resetPwdBody);

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


    @GET("/api/v2/users/{userId}/moments")
    Observable<MomentListResponse> getUserMomentsRx(@Path("userId") String userId, @Query("cursor") int cursor);

    @GET("/api/moments/recommend")
    Observable<MomentListResponse> getRecommendedMomentsRx(@Query("count") int count);

    @GET("/api/moments/myfeed")
    Observable<MomentListResponse> getMyFeed(@Query("cursor") long cursor, @Query("count") int count, @Query("order") String order, @Query("showPic") boolean showPic);


    @GET("/api/moments/{momentId}")
    Observable<MomentInfo> getMomentInfoRx(@Path("momentId") long momentId);

    @GET("/api/friends/{userId}")
    Observable<FollowInfo> getFollowInfoRx(@Path("userId") String userId);


    @GET("/api/users/{userId}")
    Call<UserInfo> getUserInfo(@Path("userId") String userId);

    @GET("/api/users/{userId}")
    Observable<UserInfo> getUserInfoRx(@Path("userId") String userId);

    @GET("/api/users/{userId}/moments/amount")
    Observable<MomentAmount> getUserMomentAmountRx(@Path("userId") String userId);

    @GET("/api/moments/summary")
    Observable<MomentSummaryResponse> getMomentSummaryRx();

    @GET("/api/users/me")
    Call<UserInfo> getMyUserInfo();

    @GET("/api/users/me")
    Observable<UserInfo> getMyUserInfoRx();

    @GET("/api/share/accounts")
    Call<LinkedAccounts> getLinkedAccounts();

    @POST("/api/reports")
    Call<SimpleBoolResponse> report(@Body ReportMomentBody reportBody);

    @POST("/api/reports")
    Call<SimpleBoolResponse> report(@Body ReportCommentBody reportBody);

    @POST("/api/reports")
    Call<SimpleBoolResponse> report(@Body ReportUserBody reportBody);

    @POST("/api/reports")
    Call<SimpleBoolResponse> report(@Body ReportFeedbackBody reportBody);

    @POST("/api/users/signin")
    Call<AuthorizeResponse> signin(@Body SignInPostBody signInPostBody);

    @POST("/api/users/signin")
    Observable<AuthorizeResponse> signinRx(@Body SignInPostBody signInPostBody);


    @POST("/api/users/signup")
    Observable<AuthorizeResponse> signUpRx(@Body SignUpPostBody signUpnPostBody);

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


    @POST("/api/moments/picture/finish")
    Call<SimpleBoolResponse> finishUploadPictureMoment(@Body FinishUploadBody finishUploadBody);

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

    @GET("/api/devices/logout")
    Observable<SimpleBoolResponse> deviceLogoutRx();

    @POST("/api/moments/update/{momentId}")
    Call<SimpleBoolResponse> updateMoment(@Path("momentId") long momentId,
                                          @Body MomentUpdateBody momentUpdatebody);



    @GET("/api/musics/categories")
    Observable<MusicCategoryResponse> getMusicCategoriesRx();

    @GET("/api/musics/categories/{categoryId}")
    Observable<MusicList> getMusicList(@Path("categoryId") long categoryId);

    @GET("/api/vehicle")
    Call<VinQueryResponse> queryByVin(@Query("vin") String vin);

    @GET("/api/vehicle")
    Observable<VinQueryResponse> queryByVinRx(@Query("vin") String vin);

    @GET("/api/moments/race")
    Observable<RaceQueryResponse> queryRaceRx(@Query("mode") int mode, @Query("start") int start,
                                      @Query("end") int end, @Query("upper") Long upper,
                                      @Query("lower") Long lower, @Query("maker") String maker,
                                      @Query("model") String model, @Query("count") int count);

    @GET("/api/place")
    Call<GeoInfoResponse> getGeoInfo(@Query("lon") double lon, @Query("lat") double lat);

    @GET("/api/place")
    Observable<GeoInfoResponse> getGeoInfoRx(@Query("lon") double lon, @Query("lat") double lat);

    @GET("api/vehicle/makers")
    Observable<MakerResponse> getAllMarkerRx();


    @GET("api/vehicle/models")
    Observable<ModelResponse> getModelByMakerRx(@Query("maker") long makerID);

    @GET("api/vehicle/years")
    Observable<ModelYearResponse> getModelYearRx(@Query("model") long model);

    @GET("api/users/vehicle")
    Observable<VehicleListResponse> getUserVehicleListRx(@Query("userID") String userId);

    @POST("api/users/vehicle")
    Observable<SimpleBoolResponse> addUserVehicle(@Body AddVehicleBody addVehicleBody);

    @DELETE("api/users/vehicle/{modelYearID}")
    Observable<SimpleBoolResponse> deleteVehicle(@Path("modelYearID") long modelYearID);

    @POST("api/devices/login")
    Observable<AuthorizeResponse> deviceLoginRx(@Body DeviceLoginBody loginBody);

    @GET("api/region/countries")
    Observable<CountryListResponse> getCountryListRx();

    @GET("api/region/cities")
    Observable<CityList> getCityListRx(@Query("cc") String countryCod, @Query("limit") int count);

    @POST("api/users/me/profile")
    Observable<SimpleBoolResponse> changeProfileRx(@Body UserProfileBody userProfileBody);

    @POST("api/users/change_password")
    Observable<AuthorizeResponse> changePasswordRx(@Body ChangePwdBody changePwdBody);

    @GET("api/moments")
    Observable<MomentListResponse> getAllMomentsRx(@Query("cursor") long cursor, @Query("count") int count,
                                                   @Query("order") String order, @Query("filter") String filter,
                                                   @Query("showPic") boolean showPic);

    @GET("api/camera/firmware")
    Observable<List<Firmware>> getFirmwareRx();

    @GET("api/moments/search")
    Observable<MomentListResponse> searchMomentRx(@Query("key") String key, @Query("count") int count);

    @GET("api/comments")
    Observable<CommentListResponse> getCommentsRx(@Query("m") long momentId,
                                                  @Query("cursor") int cursor, @Query("count") int count);

    @POST("api/comments")
    Observable<PublishCommentResponse> publishCommentRx(@Body PublishCommentBody publishCommentBody);

    @GET("api/events/comments")
    Observable<NotificationResponse> getCommentNotificationRx(@Query("cursor") long cursor, @Query("count") int count);

    @GET("api/events/likes")
    Observable<NotificationResponse> getLikeNotificationRx(@Query("cursor") long cursor, @Query("count") int count);

    @GET("api/events/shares")
    Observable<NotificationResponse> getShareNotificationRx(@Query("cursor") long cursor, @Query("count") int count);

    @GET("api/events/follows")
    Observable<NotificationResponse> getFollowNotificationRx(@Query("cursor") long cursor, @Query("count") int count);

    @POST("api/moments/views")
    Observable<SimpleBoolResponse> addViewCount(@Body AddMomentViewCountBody viewCountBody);

}
