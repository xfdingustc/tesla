package com.waylens.hachi.session;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.app.PerferenceConstant;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Xiaofei on 2015/8/6.
 */
public class SessionManager {
    private static final String TAG = SessionManager.class.getSimpleName();
    public static final String ANONYMOUS = "anonymous";

    private static SessionManager mSharedManager = null;
    private RequestQueue mRequestQueue;

    private String mUserName;
    private String mUserId;
    private boolean mHasLogined;
    private String mToken;
    private String mAvatarUrl;


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
        }
        return mSharedManager;
    }


    public static void initialize(Context context) {
        mSharedAppContext = context;
    }

    public String getUserName() {
        return mUserName;
    }

    public boolean isHasLogined() {
        return mHasLogined;
    }

    public void setHasLogined(boolean mHasLogined) {
        this.mHasLogined = mHasLogined;
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
        setHasLogined(true);
        try {
            JSONObject userInfo = response.getJSONObject(JsonKey.USER);
            String userName = userInfo.optString(JsonKey.USERNAME);
            String userId = userInfo.getString(JsonKey.USER_ID);
            String avatarUrl = userInfo.getString(JsonKey.AVATAR_URL);


            String token = response.getString(JsonKey.TOKEN);

            this.mUserName = userName;
            this.mUserId = userId;
            this.mToken = token;
            this.mAvatarUrl = avatarUrl;

            SharedPreferences share = mSharedAppContext.getSharedPreferences(PerferenceConstant.PERFERENCE,
                Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = share.edit();
            if (userName != null) {
                editor.putString(PerferenceConstant.USER_NAME, userName);
            }
            editor.putString(PerferenceConstant.USER_ID, userId);
            editor.putString(PerferenceConstant.TOKEN, token);
            editor.putString(PerferenceConstant.AVATAR_URL, avatarUrl);
            editor.commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    public void reloadLoginInfo() {
        SharedPreferences share = mSharedAppContext.getSharedPreferences(PerferenceConstant
            .PERFERENCE, Context.MODE_PRIVATE);
        this.mUserName = share.getString(PerferenceConstant.USER_NAME, null);
        this.mUserId = share.getString(PerferenceConstant.USER_ID, ANONYMOUS);
        this.mToken = share.getString(PerferenceConstant.TOKEN, null);
        if (mUserName != null && mUserId != null && mToken != null) {
            mHasLogined = true;
            Logger.t(TAG).d("Reload login info user name = " + mUserName + " user id = " +
                mUserId + " token = " + mToken);
        }
        this.mAvatarUrl = share.getString(PerferenceConstant.AVATAR_URL, null);
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
}
