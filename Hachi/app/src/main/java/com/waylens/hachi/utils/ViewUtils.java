package com.waylens.hachi.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.waylens.hachi.app.Hachi;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class ViewUtils {



    public static int getScreenWidth(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    public static int getScreenHeight(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }

    public static DisplayMetrics getDisplayMetrics(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm;
    }

    public static float getDensity() {
        return Hachi.getContext().getResources().getDisplayMetrics().density;
    }



    public static float dp2px(float dp) {
        return getDensity() * dp;
    }



    public static float sp2px(Context context, int sp) {
        float scale = context.getResources().getDisplayMetrics().scaledDensity;
        return scale * sp;
    }
    public static int dp2px(int dp) {
        Resources resources = Hachi.getContext().getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics) + 0.5f);
    }
}
