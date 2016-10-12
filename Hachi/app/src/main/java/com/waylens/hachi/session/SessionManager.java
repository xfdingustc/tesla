package com.waylens.hachi.session;

import android.content.Context;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.SocialProvider;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.rest.response.AuthorizeResponse;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.ui.authorization.VerifyEmailActivity;
import com.waylens.hachi.utils.PreferenceUtils;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Xiaofei on 2015/8/6.
 */
public class SessionManager {
    private static final String TAG = SessionManager.class.getSimpleName();
    public static final String ANONYMOUS = "anonymous";

    public static final int LOGIN_TYPE_USERNAME_PASSWORD = 1;
    public static final int LOGIN_TYPE_SNS = 2;

    private static SessionManager mSharedManager = null;


    private int mLoginType;


    private SessionManager() {

    }


    public void reloadVerifyInfo() {
        HachiApi hachiApi = HachiService.createHachiApiService();
        Call<UserInfo> userInfoCall = hachiApi.getMyUserInfo();
        userInfoCall.enqueue(new Callback<UserInfo>() {
            @Override
            public void onResponse(Call<UserInfo> call, Response<UserInfo> response) {
                Logger.t(TAG).d("userinfo: " + response.body().toString());
                saveLoginInfo(response.body());
            }

            @Override
            public void onFailure(Call<UserInfo> call, Throwable t) {

            }
        });
    }


    public static SessionManager getInstance() {
        if (mSharedManager == null) {
            mSharedManager = new SessionManager();
            mSharedManager.reloadLoginInfo();
        }
        return mSharedManager;
    }


    public static void initialize(Context context) {
        //
    }


    public void setUserName(String userName) {
        PreferenceUtils.putString(PreferenceUtils.USER_NAME, userName);
    }

    public String getUserName() {
        return PreferenceUtils.getString(PreferenceUtils.USER_NAME, null);
    }


    private void setUserId(String userId) {
        PreferenceUtils.putString(PreferenceUtils.USER_ID, userId);
    }

    public String getUserId() {
        return PreferenceUtils.getString(PreferenceUtils.USER_ID, null);
    }

    public void setToken(String token) {
        PreferenceUtils.putString(PreferenceUtils.TOKEN, token);
    }

    public String getToken() {
        return PreferenceUtils.getString(PreferenceUtils.TOKEN, null);
    }

    public void setEmail(String email) {
        PreferenceUtils.putString(PreferenceUtils.EMAIL, email);
    }

    public String getEmail() {
        return PreferenceUtils.getString(PreferenceUtils.EMAIL, null);
    }

    private void setAvatar(String avatarUrl) {
        PreferenceUtils.putString(PreferenceUtils.AVATAR_URL, avatarUrl);
    }

    private void setAvatarThumbnail(String avatarThumbnail) {
        PreferenceUtils.putString(PreferenceUtils.AVATAR_URL_THUMBNAIL, avatarThumbnail);
    }

    public String getAvatarUrl() {
        String avatarUrl = PreferenceUtils.getString(PreferenceUtils.AVATAR_URL_THUMBNAIL, null);
        if (avatarUrl != null) {
            return avatarUrl;
        } else {
            return PreferenceUtils.getString(PreferenceUtils.AVATAR_URL, null);
        }

    }

    public void setBirthday(String birthday) {
        PreferenceUtils.putString(PreferenceUtils.BIRTHDAY, birthday);
    }

    public String getBirthday() {
        return PreferenceUtils.getString(PreferenceUtils.BIRTHDAY, null);
    }

    public void setVehicle(String vehicle) {
        PreferenceUtils.putString(PreferenceUtils.VEHICLE, vehicle);
    }

    public String getVehicle() {
        return PreferenceUtils.getString(PreferenceUtils.VEHICLE, null);
    }

    public boolean isLoggedIn() {
        if (getUserName() == null || getUserId() == null || getToken() == null) {
            return false;
        } else {
            return true;
        }
    }

    public void setGender(String gender) {
        int genderInt = -1;
        if (gender.equals("MALE")) {
            genderInt = 0;
        } else if (gender.equals("FEMALE")) {
            genderInt = 1;
        }
        setGender(genderInt);
    }

    public void setGender(int gender) {
        PreferenceUtils.putInt(PreferenceUtils.GENDER, gender);
    }

    public String getGender() {
        int gender = PreferenceUtils.getInt(PreferenceUtils.GENDER, -1);
        switch (gender) {
            case 0:
                return "MALE";
            case 1:
                return "FEMALE";
            default:
                return "NA";
        }
    }

    public int getGenderInt() {
        return PreferenceUtils.getInt(PreferenceUtils.GENDER, -1);
    }

    private void setIsLinked(boolean isLinked) {
        PreferenceUtils.putBoolean(PreferenceUtils.IS_LINKED, isLinked);
    }

    public boolean getIsLinked() {
        return PreferenceUtils.getBoolean(PreferenceUtils.IS_LINKED, false);
    }

    public void setIsVerified(boolean isVerified) {
        PreferenceUtils.putBoolean(PreferenceUtils.IS_VERIFIED, isVerified);
    }

    public boolean isVerified() {
        return PreferenceUtils.getBoolean(PreferenceUtils.IS_VERIFIED, false);
    }


    private void setFacebookName(String facebookName) {
        PreferenceUtils.putString(PreferenceUtils.FACEBOOK_USER_NAME, facebookName);
    }

    public String getFacebookName() {
        return PreferenceUtils.getString(PreferenceUtils.FACEBOOK_USER_NAME, null);
    }

    private void setYoutubeName(String youtubeName) {
        PreferenceUtils.putString(PreferenceUtils.YOUTUBE_USER_NAME, youtubeName);
    }

    public String getYoutubeName() {
        return PreferenceUtils.getString(PreferenceUtils.YOUTUBE_USER_NAME, null);
    }

    private void setIsFacebookLinked(boolean linked) {
        PreferenceUtils.putBoolean(PreferenceUtils.SOCIAL_FACEBOOK_LINKED, linked);
    }

    public boolean isFacebookLinked() {
        return PreferenceUtils.getBoolean(PreferenceUtils.SOCIAL_FACEBOOK_LINKED, false);
    }

    private void setIsYoutubeLinked(boolean linked) {
        PreferenceUtils.putBoolean(PreferenceUtils.SOCIAL_YOUTUBE_LINKED, linked);
    }

    public boolean isYoutubeLinked() {
        return PreferenceUtils.getBoolean(PreferenceUtils.SOCIAL_YOUTUBE_LINKED, false);
    }

    public void setRegion(String region) {
        PreferenceUtils.putString(PreferenceUtils.REGION, region);
    }

    public String getRegion() {
        return PreferenceUtils.getString(PreferenceUtils.REGION, null);
    }


    public void saveLoginInfo(JSONObject response) {
        saveLoginInfo(response, false);
    }

    public void saveLoginInfo(JSONObject response, boolean isLoginWithSNS) {
        Logger.t(TAG).json(response.toString());
        try {
            JSONObject userInfo = response.getJSONObject(JsonKey.USER);

            if (isLoginWithSNS) {
                mLoginType = LOGIN_TYPE_SNS;
            } else {
                mLoginType = LOGIN_TYPE_USERNAME_PASSWORD;
            }


            setUserName(userInfo.optString(JsonKey.USERNAME));
            setUserId(userInfo.getString(JsonKey.USER_ID));
            setToken(response.getString(JsonKey.TOKEN));
            setAvatar(userInfo.getString(JsonKey.AVATAR_URL));
            setIsLinked(response.optBoolean(JsonKey.IS_LINKED, getIsLinked()));
            setIsVerified(userInfo.getBoolean(JsonKey.IS_VERIFIED));


            PreferenceUtils.putInt(PreferenceUtils.LOGIN_TYPE, mLoginType);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void saveLoginInfo(UserInfo userinfo) {
        setIsVerified(userinfo.isVerified);
    }

    public void saveLoginInfo(AuthorizeResponse response) {
        saveLoginInfo(response, false);
    }

    public void saveLoginInfo(AuthorizeResponse response, boolean isLoginWithSNS) {
        if (response == null) {
            return;
        }

        if (isLoginWithSNS) {
            mLoginType = LOGIN_TYPE_SNS;
        } else {
            mLoginType = LOGIN_TYPE_USERNAME_PASSWORD;
        }


        setUserName(response.user.userName);
        setUserId(response.user.userID);
        setToken(response.token);
        setAvatar(response.user.avatarUrl);
        setIsLinked(response.isLinked);
        setIsVerified(response.user.isVerified);


        PreferenceUtils.putInt(PreferenceUtils.LOGIN_TYPE, mLoginType);

    }


    public void reloadLoginInfo() {
        mLoginType = PreferenceUtils.getInt(PreferenceUtils.LOGIN_TYPE, LOGIN_TYPE_USERNAME_PASSWORD);

    }


    public boolean isCurrentUser(String userName) {
        String currentUserName = getUserName();
        if (userName.equals(currentUserName)) {
            return true;
        }

        return false;
    }


    public static boolean checkUserVerified(final Context context) {
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isVerified()) {
            return true;
        } else {
            HachiApi mHachi = HachiService.createHachiApiService();
            Call<UserInfo> userInfoCall = mHachi.getUserInfo(sessionManager.getUserId());
            userInfoCall.enqueue(new Callback<UserInfo>() {
                @Override
                public void onResponse(Call<UserInfo> call, retrofit2.Response<UserInfo> response) {
                    if (response.body() != null) {
                        SessionManager sessionManager = SessionManager.getInstance();
                        sessionManager.setIsVerified(response.body().isVerified);
                        Logger.t(TAG).d("isVerified = " + response.body().isVerified, this);

                    }
                }

                @Override
                public void onFailure(Call<UserInfo> call, Throwable t) {

                }
            });
            MaterialDialog dialog = new MaterialDialog.Builder(context)
                .content(R.string.verify_email_address)
                .positiveText(R.string.verify)
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        VerifyEmailActivity.launch(context);
                    }
                })
                .show();
            return false;
        }
    }


    public void updateLinkStatus(String userName, boolean isLinked) {
        setUserName(userName);
        setIsLinked(isLinked);
    }


    public void saveUserProfile(UserInfo userInfo) {
        setUserName(userInfo.userName);
        setEmail(userInfo.email);
        setAvatar(userInfo.avatarUrl);
        setGender(userInfo.gender);
        setBirthday(userInfo.birthday);
        setIsVerified(userInfo.isVerified);
        setRegion(userInfo.region);
        setAvatarThumbnail(userInfo.avatarThumbnailUrl);
    }

    public void saveLinkedAccounts(LinkedAccounts linkedAccounts) {
        boolean facebookFound = false;
        boolean youtubeFound = false;
        for (LinkedAccounts.LinkedAccount linkedAccount : linkedAccounts.linkedAccounts) {
            Logger.t(TAG).d("account name: " + linkedAccount.accountName);
            if (linkedAccount.provider.equals(SocialProvider.FACEBOOK)) {
                setFacebookName(linkedAccount.accountName);
                setIsFacebookLinked(true);
                facebookFound = true;
            } else if (linkedAccount.provider.equals(SocialProvider.YOUTUBE)) {
                setYoutubeName(linkedAccount.accountName);
                setIsYoutubeLinked(true);
                youtubeFound = true;
            }
        }

        if (!facebookFound) {
            setFacebookName(null);
            setIsFacebookLinked(false);
        }
        if (!youtubeFound) {
            setYoutubeName(null);
            setIsYoutubeLinked(false);
        }
    }


    public void logout() {
        mLoginType = LOGIN_TYPE_USERNAME_PASSWORD;
        PreferenceUtils.remove(PreferenceUtils.USER_ID);
        PreferenceUtils.remove(PreferenceUtils.AVATAR_URL);
        PreferenceUtils.remove(PreferenceUtils.AVATAR_URL_THUMBNAIL);
        PreferenceUtils.remove(PreferenceUtils.LOGIN_TYPE);
        PreferenceUtils.remove(PreferenceUtils.IS_LINKED);
        PreferenceUtils.remove(PreferenceUtils.IS_VERIFIED);
        PreferenceUtils.remove(PreferenceUtils.BIRTHDAY);
        PreferenceUtils.remove(PreferenceUtils.VEHICLE);
        PreferenceUtils.remove(PreferenceUtils.YOUTUBE_USER_NAME);
        PreferenceUtils.remove(PreferenceUtils.SOCIAL_YOUTUBE_LINKED);
        PreferenceUtils.remove(PreferenceUtils.FACEBOOK_USER_NAME);
        PreferenceUtils.remove(PreferenceUtils.SOCIAL_FACEBOOK_LINKED);

        if (AccessToken.getCurrentAccessToken() != null) {
            LoginManager.getInstance().logOut();
        }

        HachiApi hachiApi = HachiService.createHachiApiService();
        hachiApi.deviceLogout().enqueue(new Callback<SimpleBoolResponse>() {
            @Override
            public void onResponse(Call<SimpleBoolResponse> call, Response<SimpleBoolResponse> response) {
                Logger.t(TAG).d("device logout " + response.body().result);
                PreferenceUtils.remove(PreferenceUtils.TOKEN);
                PreferenceUtils.remove(PreferenceUtils.SEND_GCM_TOKEN_SERVER);
            }

            @Override
            public void onFailure(Call<SimpleBoolResponse> call, Throwable t) {
                Logger.t(TAG).d("device layout failed");
            }
        });


    }


}
