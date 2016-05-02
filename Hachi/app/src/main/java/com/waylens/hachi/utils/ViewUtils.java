package com.waylens.hachi.utils;

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

/**
 * Copied from Android View
 * Created by Richard on 8/25/15.
 */
public class ViewUtils {

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    public static int generateViewId() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            return View.generateViewId();
        } else {
            for (; ; ) {
                final int result = sNextGeneratedId.get();
                // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
                int newValue = result + 1;
                if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
                if (sNextGeneratedId.compareAndSet(result, newValue)) {
                    return result;
                }
            }
        }
    }

    public static final int INDEX_DOT = 10;
    public static final int INDEX_MINUS = 11;
    private static NumberFormat FMT = NumberFormat.getNumberInstance(Locale.US);

    public static int[] getDigits(float number, int fractionDigits) {
        FMT.setMaximumFractionDigits(fractionDigits);
        String numberString = null;
        try {
            numberString = FMT.format(number);
        } catch (Exception e) {
            Log.e("ViewUtils", "", e);
            return new int[0];
        }
        int[] digits = new int[numberString.length()];
        for (int i = 0; i < digits.length; i++) {
            char ch = numberString.charAt(i);
            if (ch == '.') {
                digits[i] = INDEX_DOT;
            } else if (ch == '-') {
                digits[i] = INDEX_MINUS;
            } else {
                digits[i] = Character.digit(ch, 10);
            }
        }
        return digits;
    }

    public static int[] getDigits(float number) {
        return getDigits(number, 0);
    }

    public static int dp2px(int dp) {
        Resources resources = Hachi.getContext().getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics) + 0.5f);
    }
}
