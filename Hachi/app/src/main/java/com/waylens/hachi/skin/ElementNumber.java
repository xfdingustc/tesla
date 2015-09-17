package com.waylens.hachi.skin;

import android.graphics.Bitmap;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Xiaofei on 2015/9/16.
 */
public class ElementNumber extends Element {
    private static String TAG_FONT = "Font";
    private static String mFontName;

    public ElementNumber() {
        mType = ELEMENT_TYPE_NUMBER_VIEW;
    }

    public String getFontName() {
        return mFontName;
    }

    @Override
    public Bitmap getResource() {
        return null;
    }

    @Override
    public void parse(JSONObject object) {
        super.parse(object);
        try {
            mFontName = object.getString(TAG_FONT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
