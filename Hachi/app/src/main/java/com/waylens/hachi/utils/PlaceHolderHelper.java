package com.waylens.hachi.utils;

import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2016/10/28.
 */

public class PlaceHolderHelper {
    public static int getMomentThumbnailPlaceHolder() {
        if (ThemeHelper.isDarkTheme()) {
            return R.drawable.stroke_rect_dark;
        } else {
            return R.drawable.stroke_rect;
        }
    }
}
