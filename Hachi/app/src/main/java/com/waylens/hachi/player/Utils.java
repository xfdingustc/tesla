package com.waylens.hachi.player;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.google.android.exoplayer.ExoPlayerLibraryInfo;

/**
 * Created by Xiaofei on 2016/6/23.
 */
public class Utils {
    public static String getUserAgent(Context context, String applicationName) {
        String versionName;
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "?";
        }
        return applicationName + "/" + versionName + " (Linux;Android " + Build.VERSION.RELEASE
            + ") " + "ExoPlayerLib/" + ExoPlayerLibraryInfo.VERSION;
    }
}
