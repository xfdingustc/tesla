package com.waylens.hachi.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.waylens.hachi.view.gauge.GaugeView;

/**
 * Created by Xiaofei on 2016/11/22.
 */

public class BitmapUtils {
    public static Bitmap getBitmapFromView(GaugeView v) {
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.RGB_565);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }
}
