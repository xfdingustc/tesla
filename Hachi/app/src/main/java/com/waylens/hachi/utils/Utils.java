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

	public static float getDensity(Context context) {
		return context.getResources().getDisplayMetrics().density;
	}

	public static float dp2Px(int dp) {
		float scale = Hachi.getContext().getResources().getDisplayMetrics().density;
		return scale * dp;
	}

	public static final int dp2Px(float density, int dp) {
		return (int)(density * dp + 0.5f);
	}

	public static float sp2px(Context context, int sp) {
		float scale = context.getResources().getDisplayMetrics().scaledDensity;
		return scale * sp;
	}

	public static void measureView(View view) {
		view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0,
				MeasureSpec.UNSPECIFIED));
	}

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

	public static void setLayoutWidth(View view, int width) {
		ViewGroup.LayoutParams lp = view.getLayoutParams();
		if (lp.height != width) {
			lp.width = width;
			view.setLayoutParams(lp);
		}
	}

	public static void setLayoutHeight(View view, int height) {
		ViewGroup.LayoutParams lp = view.getLayoutParams();
		if (lp.height != height) {
			lp.height = height;
			view.setLayoutParams(lp);
		}
	}

	public static void setView16by9(Activity activity, View view) {
		MarginLayoutParams lp = (MarginLayoutParams)view.getLayoutParams();
		lp.width = LayoutParams.MATCH_PARENT;
		lp.height = Utils.getScreenWidth(activity) * 9 / 16;
		view.setLayoutParams(lp);
	}

	public static void setViewFullSize(Activity activity, View view) {
		MarginLayoutParams lp = (MarginLayoutParams)view.getLayoutParams();
		lp.width = LayoutParams.MATCH_PARENT;
		lp.height = LayoutParams.MATCH_PARENT;
		view.setLayoutParams(lp);
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

			View parentView = (View)parent;
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
		MarginLayoutParams lp = (MarginLayoutParams)view.getLayoutParams();
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




	public static int readi32(byte[] data, int index) {
		int result = (int)data[index] & 0xFF;
		result |= ((int)data[index + 1] & 0xFF) << 8;
		result |= ((int)data[index + 2] & 0xFF) << 16;
		result |= ((int)data[index + 3] & 0xFF) << 24;
		return result;
	}

	public static int read16(byte[] data, int index) {
		int result = (int)data[index] & 0xFF;
		result |= ((int)data[index + 1] & 0xFF) << 8;
		return result;
	}


}
