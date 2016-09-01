package com.waylens.hachi.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewParent;

import com.waylens.hachi.app.Hachi;

import java.math.BigDecimal;
import java.util.Locale;

public final class Utils {

    public static String normalizeNetworkName(String ssid) {
        if (ssid != null && ssid.length() > 0) {
            if (ssid.charAt(0) == '"' && ssid.charAt(ssid.length() - 1) == '"') {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            return ssid;
        }
        return null;
    }



    public static void measureView(View view) {
        view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0,
            MeasureSpec.UNSPECIFIED));
    }

    public static String getSpaceString(long space) {
        float spaceInM = ((float)space) / (1024 * 1024);

        String spaceStr;
        if (spaceInM > 1024) {
            BigDecimal tmp = new BigDecimal(spaceInM / 1024);
            spaceStr = String.valueOf(tmp.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue()) + " GB";
        } else {
            BigDecimal tmp = new BigDecimal(spaceInM);
            spaceStr = String.valueOf(tmp.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue()) + " MB";
        }
        return spaceStr;
    }

    public static String getSpaceNumber(long space) {
        float spaceInM = ((float)space) / (1024 * 1024);

        String spaceStr;
        if (spaceInM > 1024) {
            BigDecimal tmp = new BigDecimal(spaceInM / 1024);
            spaceStr = String.valueOf(tmp.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
        } else {
            BigDecimal tmp = new BigDecimal(spaceInM);
            spaceStr = String.valueOf(tmp.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
        }
        return spaceStr;
    }


    public static String getSpaceUnit(long space) {
        float spaceInM = ((float)space) / (1024 * 1024);

        String spaceStr;
        if (spaceInM > 1024) {
            spaceStr = "GB";
        } else {
            spaceStr = "MB";
        }
        return spaceStr;
    }





    public static void getViewRect(View view, Rect rect) {
        rect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
    }

    public static Rect getViewRect(View view) {
        return new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
    }

    public static void translateRect(View view, Rect rect, boolean toAbsolute) {
        while (true) {
            ViewParent parent = view.getParent();
            if (parent == null || !(parent instanceof View))
                break;

            View parentView = (View) parent;
            int left = parentView.getLeft() - parentView.getScrollX();
            int top = parentView.getTop() - parentView.getScrollY();
            if (toAbsolute) {
                rect.offset(left, top);
            } else {
                rect.offset(-left, -top);
            }

            view = parentView;
        }
    }

    // API
    public static void getViewRectForView(View v, View view, Rect rect) {
        Utils.getViewRect(v, rect);
        Utils.translateRect(v, rect, true);
        Utils.translateRect(view, rect, false);
    }

    // TODO - getLocationInWindow() ?
    public static void positionViewTo(View view, View target) {
        Rect rect = getViewRect(target);
        translateRect(target, rect, true);
        translateRect(view, rect, false);
        MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
        if (lp.width != rect.width() || lp.height != rect.height()) {
            lp.width = rect.width();
            lp.height = rect.height();
            lp.setMargins(rect.left, rect.top, 0, 0);
            view.setLayoutParams(lp);
        }
    }

    public static String formatSpace(int kb) {
        if (kb >= 10 * 1000 * 1000) { // 10.x GB
            return String.format(Locale.US, "%.01f GB", (double) kb / (1 * 1000 * 1000));
        }
        if (kb >= 1 * 1000 * 1000) { // 1.xx GB
            return String.format(Locale.US, "%.02f GB", (double) kb / (1 * 1000 * 1000));
        }
        if (kb <= 1 * 1000 && kb > 0) {
            return "1 MB";
        }
        return String.format(Locale.US, "%d MB", kb / 1000);
    }

    public static int toggleBit(int flags, int bit) {
        if ((flags & bit) != 0) {
            flags &= ~bit;
        } else {
            flags |= bit;
        }
        return flags;
    }

    public static String getStringFromArray(int index, String[] list) {
        if (index < 0 || index >= list.length)
            index = 0;
        else
            index++;
        return list[index];
    }


}
