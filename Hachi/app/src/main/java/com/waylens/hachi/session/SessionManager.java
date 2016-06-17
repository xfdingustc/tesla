package com.waylens.hachi.session;

import android.content.Context;
import android.preference.PreferenceFragment;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.orhanobut.logger.Logger;
import com.rest.HachiApi;
import com.rest.HachiService;
import com.rest.response.SignInResponse;
import com.rest.response.UserInfo;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.app.JsonKey;
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

    private String mUserName;
    private String mUserId;
    private boolean mHasLogined;
    private String mToken;
    private String mAvatarUrl;
    private String mEmail;
    private boolean mIsVerified;

    private int mLoginType;
    private boolean mIsLinked;
    private String mBirthday;

    private int mGender;
    private String mRegion;

    private SessionManager() {
        resetSessionInfo();
    }

    public void saveUserName(String newUserName) {
        mUserName = newUserName;
        PreferenceUtils.putString(PreferenceUtils.USER_NAME, mUserName);
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


    private void resetSessionInfo() {
        this.mUserName = null;
        this.mUserId = ANONYMOUS;
        this.mHasLogined = false;
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

    public String getUserName() {
        return mUserName;
    }

    public boolean isLoggedIn() {
        return mHasLogined;
    }

    public void setIsLoggedIn(boolean isLoggedIn) {
        mHasLogined = isLoggedIn;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getBirthday() {
        return mBirthday;
    }

    public String getRegion() {
        return PreferenceUtils.getString(PreferenceUtils.REGION, null);
    }

    public int getGender() {
        return mGender;
    }

    public String getToken() {
        return mToken;
    }


    public String getUserId() {
        return mUserId;
    }

    public String getAvatarUrl() {
        return mAvatarUrl;
    }

    public void saveLoginInfo(JSONObject response) {
        saveLoginInfo(response, false);
    }

    public void saveLoginInfo(JSONObject response, boolean isLoginWithSNS) {
        Logger.t(TAG).json(response.toString());
        try {
            JSONObject userInfo = response.getJSONObject(JsonKey.USER);
            mUserName = userInfo.optString(JsonKey.USERNAME);
            mUserId = userInfo.getString(JsonKey.USER_ID);
            mAvatarUrl = userInfo.getString(JsonKey.AVATAR_URL);

            mIsVerified = userInfo.getBoolean(JsonKey.IS_VERIFIED);

            mToken = response.getString(JsonKey.TOKEN);
            mIsLinked = response.optBoolean(JsonKey.IS_LINKED);

            if (isLoginWithSNS) {
                mLoginType = LOGIN_TYPE_SNS;
            } else {
                mLoginType = LOGIN_TYPE_USERNAME_PASSWORD;
            }

            setLoginInternal();

            PreferenceUtils.putString(PreferenceUtils.USER_NAME, mUserName);
            PreferenceUtils.putString(PreferenceUtils.USER_ID, mUserId);
            PreferenceUtils.putString(PreferenceUtils.TOKEN, mToken);
            PreferenceUtils.putString(PreferenceUtils.AVATAR_URL, mAvatarUrl);

            PreferenceUtils.putInt(PreferenceUtils.LOGIN_TYPE, mLoginType);
            PreferenceUtils.putBoolean(PreferenceUtils.IS_LINKED, mIsLinked);
            PreferenceUtils.putBoolean(PreferenceUtils.IS_VERIFIED, mIsVerified);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void saveLoginInfo(UserInfo userinfo) {
        mIsVerified = userinfo.isVerified;
        PreferenceUtils.putBoolean(PreferenceUtils.IS_VERIFIED, mIsVerified);
    }

    public void saveLoginInfo(SignInResponse response) {
        saveLoginInfo(response, false);
    }

    public void saveLoginInfo(SignInResponse response, boolean isLoginWithSNS) {
        if (response == null) {
            return;
        }
        mUserName = response.user.userName;
        mUserId = response.user.userID;
        mAvatarUrl = response.user.avatarUrl;
        mToken = response.token;
        mIsLinked = false;

        mIsVerified = response.user.isVerified;

        if(isLoginWithSNS) {
            mLoginType = LOGIN_TYPE_SNS;
        } else {
            mLoginType = LOGIN_TYPE_USERNAME_PASSWORD;
        }

        setLoginInternal();

        PreferenceUtils.putString(PreferenceUtils.USER_NAME, mUserName);
        PreferenceUtils.putString(PreferenceUtils.USER_ID, mUserId);
        PreferenceUtils.putString(PreferenceUtils.TOKEN, mToken);
        PreferenceUtils.putString(PreferenceUtils.AVATAR_URL, mAvatarUrl);
        PreferenceUtils.putInt(PreferenceUtils.LOGIN_TYPE, mLoginType);
        PreferenceUtils.putBoolean(PreferenceUtils.IS_LINKED, mIsLinked);
        PreferenceUtils.putBoolean(PreferenceUtils.IS_VERIFIED, mIsVerified);
    }




    public void reloadLoginInfo() {
        this.mUserName = PreferenceUtils.getString(PreferenceUtils.USER_NAME, null);
        this.mUserId = PreferenceUtils.getString(PreferenceUtils.USER_ID, ANONYMOUS);
        this.mToken = PreferenceUtils.getString(PreferenceUtils.TOKEN, null);
        this.mAvatarUrl = PreferenceUtils.getString(PreferenceUtils.AVATAR_URL, null);
        this.mEmail = PreferenceUtils.getString(PreferenceUtils.EMAIL, null);
        mIsLinked = PreferenceUtils.getBoolean(PreferenceUtils.IS_LINKED, false);
        mLoginType = PreferenceUtils.getInt(PreferenceUtils.LOGIN_TYPE, LOGIN_TYPE_USERNAME_PASSWORD);
        mIsVerified = PreferenceUtils.getBoolean(PreferenceUtils.IS_VERIFIED, false);
        setLoginInternal();
    }

    private void setLoginInternal() {
        if (mUserName != null && mUserId != null && mToken != null) {
            setIsLoggedIn(true);
        } else {
            setIsLoggedIn(false);
        }
    }

    public boolean isLinked() {
        mIsLinked = PreferenceUtils.getBoolean(PreferenceUtils.IS_LINKED, false);
        return mIsLinked;
    }

    public boolean isVerified() {
        mIsVerified = PreferenceUtils.getBoolean(PreferenceUtils.IS_VERIFIED, false);
        return mIsVerified;
    }

    public void setIsVerified(boolean isVerified) {
        mIsVerified = isVerified;
        PreferenceUtils.putBoolean(PreferenceUtils.IS_VERIFIED, isVerified);
    }

    public boolean needLinkAccount() {
        return (mLoginType == LOGIN_TYPE_SNS) && !mIsLinked;
    }

    public void updateLinkStatus(String userName, boolean isLinked) {
        mUserName = userName;
        mIsLinked = isLinked;
        PreferenceUtils.putString(PreferenceUtils.USER_NAME, userName);
        PreferenceUtils.putBoolean(PreferenceUtils.IS_LINKED, isLinked);
        setLoginInternal();
    }

    public void saveUserProfile(JSONObject response) {
        try {
            mUserName = response.getString("userName");
            mEmail = response.getString("email");
            mAvatarUrl = response.getString("avatarUrl");
            mBirthday = response.getString("birthday");
            mRegion = response.getString("region");
            String gender = response.getString("gender");
            if (gender.equals("MALE")) {
                mGender = 0;
            } else if (gender.equals("FEMALE")) {
                mGender = 1;
            } else {
                mGender = -1;
            }



            PreferenceUtils.putString(PreferenceUtils.USER_NAME, mUserName);
            PreferenceUtils.putString(PreferenceUtils.EMAIL, mEmail);
            PreferenceUtils.putString(PreferenceUtils.AVATAR_URL, mAvatarUrl);
            PreferenceUtils.putString(PreferenceUtils.BIRTHDAY, mBirthday);
            PreferenceUtils.putInt(PreferenceUtils.GENDER, mGender);
            PreferenceUtils.putString(PreferenceUtils.REGION, mRegion);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    public void refreshUserProfile() {
        /*
        String url = Constant.HOST_URL + Constant.USER_PROFILE;

        TutuJsonObjectRequest refreshRequest = new TutuJsonObjectRequest(url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                updateUserProfile(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        mRequestQueue.add(refreshRequest);
        mRequestQueue.start();
*/
    }

    public void clearLoginInfo() {
        /*
        SharedPreferences share = mSharedAppContext.getSharedPreferences(PerferenceConstant.PERFERENCE,
            Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = share.edit();
        editor.remove(PerferenceConstant.USER_NAME);
        editor.remove(PerferenceConstant.USER_ID);
        editor.remove(PerferenceConstant.TOKEN);
        editor.remove(PerferenceConstant.REFRESH_TOKEN);
        editor.remove(PerferenceConstant.AVATAR_URL);
        editor.commit();
        resetSessionInfo();
        */
    }

    private void updateUserProfile(JSONObject response) {
        /*
        String avatarUrl = response.optString(Constant.JSON_KEY_AVATAR_URL);
        if (avatarUrl != null && !avatarUrl.equals("")) {
            SharedPreferences share = mSharedAppContext.getSharedPreferences(PerferenceConstant.PERFERENCE,
                Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = share.edit();
            editor.putString(PerferenceConstant.AVATAR_URL, avatarUrl);
            editor.commit();
        }
        */
    }

    public void logout() {
        mHasLogined = false;

        mUserId = null;
        mAvatarUrl = null;
        mToken = null;
        mIsLinked = false;
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


    public void refreshToken(String waylensToken) {
        mToken = waylensToken;
        PreferenceUtils.putString(PreferenceUtils.TOKEN, mToken);
    }
}
