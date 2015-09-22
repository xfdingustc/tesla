package com.waylens.hachi.Helper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

/**
 * Created by Xiaofei on 2015/9/22.
 */
public class UILHelper {
    private static BitmapFactory.Options getDefaultOptions() {
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inPreferredConfig = Bitmap.Config.RGB_565;
        return option;
    }

    public static DisplayImageOptions getCachedDisplayOption() {
        return new DisplayImageOptions.Builder()
            .decodingOptions(getDefaultOptions())
            .cacheInMemory(true)
            .cacheOnDisc(true)
            .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
            .build();
    }
}
