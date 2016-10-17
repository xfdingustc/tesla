package com.waylens.hachi.utils;

import android.graphics.Color;

import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2016/10/16.
 */

public class MusicCategoryHelper {
    private static int[] MUSIC_BACKGROUND_COLOR = {
        0xFF459E6F,
        0xFF3FB0C8,
        0xFF6B6CB2,
        0xFFEA6952,
        0xFFD580B6,
        0xFFFAA61A,
        0xFF3D86C6,

    };

    public static int getMusicBgColor(int index) {
        int musicColoIndex = index % (MUSIC_BACKGROUND_COLOR.length);
        return MUSIC_BACKGROUND_COLOR[musicColoIndex];
    }
}
