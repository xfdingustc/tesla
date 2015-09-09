package com.waylens.hachi.skin;

import android.graphics.Bitmap;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Xiaofei on 2015/9/8.
 */
public class ElementStaticImage extends Element {
    private static final String TAG_RESOURCE = "resource";

    @Override
    public void parse(JSONObject object) {
        try {
            String resource = object.getString(TAG_RESOURCE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Bitmap getResource() {
        return null;
    }


}
