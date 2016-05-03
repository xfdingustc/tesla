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
    public static final String EMAIL = "email";
    public static final String TOKEN = "token";
    public static final String BIRTHDAY = "birthday";
    public static final String GENDER = "gender";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String IS_FIRST_INSTALL = "is_first_install";
    public static final String VERSION_NAME = "version_name";
    public static final String VERSION_CODE = "version_code";
    public static final String APP_THEME = "app_theme";
    public static final String LOGIN_TYPE = "login_type";
    public static final String IS_LINKED = "is_linked";
    public static final String SEND_GCM_TOKEN_SERVER = "send.gcm.token.to.server";

    public static final String KEY_WEATHER_TEMP_F = "weather.temp.f";
    public static final String KEY_WEATHER_WIND_SPEED = "weather.wind.speed";
    public static final String KEY_WEATHER_ICON_URL = "weather.icon.url";
    public static final String KEY_WEATHER_UPDATE_TIME = "weather.update.time";

    public static final String KEY_SIGN_UP_EMAIL = "sign.up.email";
    public static final String KEY_RESET_EMAIL_SENT = "is.reset.email.sent";

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

    public static void putInt(String key, int value) {
        mEditor.putInt(key, value).apply();
    }

    public static void putBoolean(String key, boolean value) {
        mEditor.putBoolean(key, value).apply();
    }

    public static boolean getBoolean(String key, boolean defValue) {
        return mShare.getBoolean(key, defValue);
    }

    public static int getInt(String key, int defValue) {
        return mShare.getInt(key, defValue);
    }

    public static void remove(String key) {
        mEditor.remove(key).apply();
    }

    public static void putLong(String key, long id) {
        mEditor.putLong(key, id).apply();
    }

    public static long getLong(String key, long defaultValue) {
        return mShare.getLong(key, defaultValue);
    }
}
