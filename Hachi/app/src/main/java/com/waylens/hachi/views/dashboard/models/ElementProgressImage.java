package com.waylens.hachi.views.dashboard.models;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Xiaofei on 2015/9/10.
 */
public class ElementProgressImage extends ElementStaticImage {

    public static final int PROGRESS_IMAGE_STYLE_DEFAULT = 0;
    public static final int PROGRESS_IMAGE_STYLE_RING = 1;

    private static final String PROGRESS_IMAGE_STYLE_RING_STR = "Ring";

    private static final String TAG_MAX = "Max";
    private static final String TAG_STYLE = "Style";
    private static final String TAG_DIRECTION = "Direction";
    private static final String TAG_START_RADIUS = "StartRadius";
    private static final String TAG_END_RADIUS = "EndRadius";



    private int mStyle;

    private int mMax = 100;

    public ElementProgressImage() {
        mType = ELEMENT_TYPE_PROGRESS_IMAGE;
        mStyle = PROGRESS_IMAGE_STYLE_DEFAULT;
    }

    public int getMax() {
        return mMax;
    }

    public int getStyle() {
        return mStyle;
    }


    @Override
    public void parse(JSONObject object) {
        super.parse(object);
        mMax = object.optInt(TAG_MAX, 100);
        String style = object.optString(TAG_STYLE, null);

        mStyle = getStyleType(style);


        String direction = object.optString(TAG_DIRECTION, null);
        if (direction != null) {
            mAttributeSet.put(TAG_DIRECTION, direction);
        }

        String startRadius = object.optString(TAG_START_RADIUS, null);
        if (direction != null) {
            mAttributeSet.put(TAG_START_RADIUS, startRadius);
        }

        String endRadius = object.optString(TAG_END_RADIUS, null);
        if (endRadius != null) {
            mAttributeSet.put(TAG_END_RADIUS, endRadius);
        }
    }

    private int getStyleType(String style) {
        if (style == null) {
            return PROGRESS_IMAGE_STYLE_DEFAULT;
        } else if(style.equals(PROGRESS_IMAGE_STYLE_RING_STR)) {
            return PROGRESS_IMAGE_STYLE_RING;
        }
        return PROGRESS_IMAGE_STYLE_DEFAULT;
    }
}
