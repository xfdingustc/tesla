package com.waylens.mediatranscoder.engine;

import android.graphics.Bitmap;

/**
 * Created by Xiaofei on 2015/11/5.
 */
public interface OverlayProvider {
    Bitmap updateTexImage(long pts);
}
