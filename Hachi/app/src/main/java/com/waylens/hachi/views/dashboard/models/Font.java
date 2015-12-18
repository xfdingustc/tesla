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

/**
 * Created by Xiaofei on 2015/9/16.
 */
public class Font {

    private static final String TAG_NUMBERS = "Numbers";
    private static final String TAG_RESOURCE = "Resource";

    private Bitmap[] mNumberResources = new Bitmap[10];

    public void parse(JSONObject object) {
        try {
            JSONArray numberArray = object.getJSONArray(TAG_NUMBERS);
            Context context = Hachi.getContext();

            for (int i = 0; i < 10; i++) {
                JSONObject numberUrl = numberArray.getJSONObject(i);
                String resourceUrl = numberUrl.getString(TAG_RESOURCE);
                InputStream in = context.getAssets().open(resourceUrl);
                mNumberResources[i] = BitmapFactory.decodeStream(in);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap getNumberResource(int index) {
        return mNumberResources[index];
    }
}
