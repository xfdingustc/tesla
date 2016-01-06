package com.waylens.hachi.views.dashboard.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.Hachi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Xiaofei on 2015/9/16.
 */
public class Font {
    private static final String TAG = Font.class.getSimpleName();
    private static final String TAG_NUMBERS = "Numbers";
    private static final String TAG_RESOURCES = "Resources";
    private static final String TAG_CHARNUM = "CharNum";
    private static final String TAG_KEY = "key";
    private static final String TAG_VALUE = "value";

    private int mWidth = 0;
    private int mHeight = 0;

    private int mCharNumber = 1;

    private Map<String, Bitmap> mFontResources = new HashMap<>();

    public void parse(JSONObject object) {
        try {

            Context context = Hachi.getContext();

            mCharNumber = object.optInt(TAG_CHARNUM, 1);

            JSONArray resourceArray = object.optJSONArray(TAG_RESOURCES);
            if (resourceArray != null) {
                for (int i = 0; i < resourceArray.length(); i++) {
                    JSONObject fontElement = resourceArray.getJSONObject(i);
                    String key = fontElement.getString(TAG_KEY);
                    String value = fontElement.getString(TAG_VALUE);
//                    Logger.t(TAG).d("Add Font element: key = " + key + " value = " + value);
                    InputStream in = context.getAssets().open(value);
                    Bitmap resource = BitmapFactory.decodeStream(in);


                    if (resource != null) {
                        mFontResources.put(key, resource);
                        if (key.equals("0")) {
                            mWidth = resource.getWidth();
                            mHeight = resource.getHeight();
                        }
                    }

                }
            }

            JSONArray numberArray = object.optJSONArray(TAG_NUMBERS);
            if (numberArray != null) {
                // Add numbers:
                for (int i = 0; i < 10; i++) {
                    JSONObject numberUrl = numberArray.getJSONObject(i);
                    String resourceUrl = numberUrl.getString(String.valueOf(i));
                    InputStream in = context.getAssets().open(resourceUrl);

                    mFontResources.put(String.valueOf(i), BitmapFactory.decodeStream(in));
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getFontWidth() {
        return mWidth;
    }

    public int getFontHeight() {
        return mHeight;
    }

    public int getCharNumber() {
        return mCharNumber;
    }

    public Bitmap getFontResource(String charactor) {
        return mFontResources.get(charactor);
    }
}
