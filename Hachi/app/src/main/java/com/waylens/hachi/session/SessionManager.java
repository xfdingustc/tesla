package com.waylens.hachi.session;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.utils.PreferenceUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Xiaofei on 2015/8/6.
 */
public class SessionManager {
    private static final String TAG = SessionManager.class.getSimpleName();
    public static final String ANONYMOUS = "anonymous";

    public static final int LOGIN_TYPE_USERNAME_PASSWORD = 1;
    public static final int LOGIN_TYPE_SNS = 2;

    private static SessionManager mSharedManager = null;
    private RequestQueue mRequestQueue;

    private String mUserName;
    private String mUserId;
    private boolean mHasLogined;
    private String mToken;
    private String mAvatarUrl;

    private int mLoginType;
    private boolean mIsLinked;


    private static Context mSharedAppContext = null;


    private SessionManager() {
        resetSessionInfo();
        mRequestQueue = Volley.newRequestQueue(mSharedAppContext);
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
        mSharedAppContext = context;
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
        try {
            JSONObject userInfo = response.getJSONObject(JsonKey.USER);
            mUserName = userInfo.optString(JsonKey.USERNAME);
            mUserId = userInfo.getString(JsonKey.USER_ID);
            mAvatarUrl = userInfo.getString(JsonKey.AVATAR_URL);

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

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void reloadLoginInfo() {
        this.mUserName = PreferenceUtils.getString(PreferenceUtils.USER_NAME, null);
        this.mUserId = PreferenceUtils.getString(PreferenceUtils.USER_ID, ANONYMOUS);
        this.mToken = PreferenceUtils.getString(PreferenceUtils.TOKEN, null);
        this.mAvatarUrl = PreferenceUtils.getString(PreferenceUtils.AVATAR_URL, null);
        mIsLinked = PreferenceUtils.getBoolean(PreferenceUtils.IS_LINKED, false);
        mLoginType = PreferenceUtils.getInt(PreferenceUtils.LOGIN_TYPE, LOGIN_TYPE_USERNAME_PASSWORD);

        setLoginInternal();
    }

    private void setLoginInternal() {
        if (mUserName != null
                && mUserId != null
                && mToken != null
                && !needLinkAccount()) {
            setIsLoggedIn(true);
        } else {
            setIsLoggedIn(false);
        }
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

    }

    public void refreshlogin() {
        // TODO: uncomment this code block after refresh token is added back
    /*
    String url = Constant.HOST_URL + Constant.REFRESH_TOKEN_URL + mRefreshToken + "&id=" + mUserId;

    RequestQueue requestQueue =
    TutuJsonObjectRequest refreshRequest = new TutuJsonObjectRequest(url, new Response.Listener<JSONObject>() {
      @Override
      public void onResponse(JSONObject response) {
        saveLoginInfo(response.toString());
        Toast.makeText(mSharedAppContext, mSharedAppContext.getString(R.string
            .login_successfully), Toast.LENGTH_SHORT).show();
      }
    }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {

      }
    });

    requestQueue.add(refreshRequest);
    requestQueue.start();
    */
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

        if (AccessToken.getCurrentAccessToken() != null) {
            LoginManager.getInstance().logOut();
        }
    }


    public void refreshToken(String waylensToken) {
        mToken = waylensToken;
        PreferenceUtils.putString(PreferenceUtils.TOKEN, mToken);
    }
}
