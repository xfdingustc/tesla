package com.waylens.hachi.session;

import android.content.Context;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.SignInResponse;
import com.waylens.hachi.rest.response.UserInfo;
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


    private String mRegion;

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

    public boolean isLoggedIn() {
        if (getUserName() == null || getUserId() == null || getToken() == null) {
            return false;
        } else {
            return true;
        }
    }

    private void setGender(String gender) {
        int genderInt = -1;
        if (gender.equals("MALE")) {
            genderInt = 0;
        } else if (gender.equals("FEMALE")) {
            genderInt = 1;
        }
        setGender(genderInt);
    }

    private void setGender(int gender) {
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


    public String getRegion() {
        return PreferenceUtils.getString(PreferenceUtils.REGION, null);
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

    public void saveLoginInfo(SignInResponse response) {
        saveLoginInfo(response, false);
    }

    public void saveLoginInfo(SignInResponse response, boolean isLoginWithSNS) {
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


    public void updateLinkStatus(String userName, boolean isLinked) {
        setUserName(userName);
        setIsLinked(isLinked);
    }

    public void saveUserProfile(JSONObject response) {
        try {

            mRegion = response.getString("region");

            setUserName(response.getString("userName"));
            setEmail(response.getString("email"));
            setAvatar(response.getString("avatarUrl"));
            setBirthday(response.getString("birthday"));
            setGender(response.getString("gender"));

            PreferenceUtils.putString(PreferenceUtils.REGION, mRegion);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void saveUserProfile(UserInfo userInfo) {
        setUserName(userInfo.userName);
        setAvatar(userInfo.avatarUrl);
        setAvatarThumbnail(userInfo.avatarThumbnailUrl);
        setFacebookName(userInfo.facebookName);

    }


    public void logout() {

        mLoginType = LOGIN_TYPE_USERNAME_PASSWORD;

        PreferenceUtils.remove(PreferenceUtils.USER_ID);
        PreferenceUtils.remove(PreferenceUtils.TOKEN);
        PreferenceUtils.remove(PreferenceUtils.AVATAR_URL);
        PreferenceUtils.remove(PreferenceUtils.LOGIN_TYPE);
        PreferenceUtils.remove(PreferenceUtils.IS_LINKED);
        PreferenceUtils.remove(PreferenceUtils.IS_VERIFIED);

        if (AccessToken.getCurrentAccessToken() != null) {
            LoginManager.getInstance().logOut();
        }
    }


}
