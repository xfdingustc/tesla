package com.waylens.hachi.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Xiaofei on 2015/8/6.
 */
public class PreferenceUtils {
    public static final String PREFERENCE = "preference";
    public static final String USER_NAME = "user_name";
    public static final String AVATAR_URL = "avatar_url";
    public static final String USER_ID = "user_id";
    public static final String TOKEN = "token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String IS_FIRST_INSTALL = "is_first_install";
    public static final String VERSION_NAME = "version_name";
    public static final String VERSION_CODE = "version_code";

    private static Context mSharedAppContext = null;
    private static SharedPreferences mShare = null;
    private static SharedPreferences.Editor mEditor = null;

    public static void initialize(Context context) {
        mSharedAppContext = context;
        mShare = mSharedAppContext.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        mEditor = mShare.edit();
    }

    public static String getString(String key, String defValue) {
        return mShare.getString(key, defValue);
    }

    public static void putString(String key, String value) {
        mEditor.putString(key, value).apply();

    }

}