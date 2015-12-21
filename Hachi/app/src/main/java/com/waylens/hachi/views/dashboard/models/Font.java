package com.waylens.hachi.views.dashboard.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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

    private static final String TAG_NUMBERS = "Numbers";
    private static final String TAG_RESOURCE = "Resource";
    private static final String TAG_PUNCTUATIONS = "Punctuations";

    private Map<String, Bitmap> mFontResources = new HashMap<>();

    public void parse(JSONObject object) {
        try {
            JSONArray numberArray = object.getJSONArray(TAG_NUMBERS);
            Context context = Hachi.getContext();

            // Add numbers:
            for (int i = 0; i < 10; i++) {
                JSONObject numberUrl = numberArray.getJSONObject(i);
                String resourceUrl = numberUrl.getString(String.valueOf(i));
                InputStream in = context.getAssets().open(resourceUrl);

                mFontResources.put(String.valueOf(i), BitmapFactory.decodeStream(in));
            }

            JSONObject punctuations = object.optJSONObject(TAG_PUNCTUATIONS);
            if (punctuations != null) {
                String colonResource = punctuations.optString(":");
                if (colonResource != null) {
                    InputStream in = context.getAssets().open(colonResource);
                    mFontResources.put(":", BitmapFactory.decodeStream(in));
                }
            }



        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap getFontResource(String charactor) {
        return mFontResources.get(charactor);
    }
}
