package com.waylens.hachi.utils;

/**
 * Created by Xiaofei on 2016/10/19.
 */

public class MathUtils {

    private MathUtils() { }

    public static float constrain(float min, float max, float v) {
        return Math.max(min, Math.min(max, v));
    }
}
