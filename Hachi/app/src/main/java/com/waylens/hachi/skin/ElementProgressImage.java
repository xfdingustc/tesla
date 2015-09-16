package com.waylens.hachi.skin;

import org.json.JSONObject;

/**
 * Created by Xiaofei on 2015/9/10.
 */
public class ElementProgressImage extends ElementStaticImage {
    public ElementProgressImage() {
        mType = ELEMENT_TYPE_PROGRESS_IMAGE;
    }

    private static final String TAG_MAX = "Max";

    private int mMax = 100;

    public int getMax() {
        return mMax;
    }



    @Override
    public void parse(JSONObject object) {
        super.parse(object);
        mMax = object.optInt(TAG_MAX, 100);
    }
}
