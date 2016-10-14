package com.waylens.hachi.utils;

import android.support.annotation.ColorRes;

import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2016/10/14.
 */

public class AvatarHelper {
    private static @ColorRes int[] AVATAR_BACKGROUND_COLOR = {
        R.color.material_amber_500,
        R.color.material_blue_500,
        R.color.material_blue_grey_500,
        R.color.material_brown_500,
        R.color.material_cyan_500,
        R.color.material_green_500,
        R.color.material_indigo_500,
        R.color.material_lime_500,
        R.color.material_orange_500,
        R.color.material_red_500,
        R.color.material_pink_500,
        R.color.material_teal_500,
        R.color.material_light_blue_500,
        R.color.material_purple_500,
        R.color.material_light_green_500,
        R.color.material_deep_orange_500,
        R.color.material_yellow_500,
    };

    public static @ColorRes int getRandomAvatarBackgroundColor() {
        int resourceIndex = (int)(Math.random() * AVATAR_BACKGROUND_COLOR.length);
        return AVATAR_BACKGROUND_COLOR[resourceIndex];
    }
}
