package com.waylens.hachi.views;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Util class
 * Created by liangyx on 7/16/15.
 */
public class ViewUtils {
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

    public static int dp2px(int dp, Resources resources) {
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics) + 0.5f);
    }
}
