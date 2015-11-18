package com.waylens.hachi.skin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.waylens.hachi.app.Hachi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Xiaofei on 2015/9/8.
 */
public class ElementStaticImage extends Element {
    private static final String TAG_RESOURCE = "Resource";

    private Bitmap mBitmap = null;

    public ElementStaticImage() {
        mType = ELEMENT_TYPE_STATIC_IMAGE;
    }

    @Override
    public void parse(JSONObject object) {
        super.parse(object);
        try {
            mResourceUrl = object.getString(TAG_RESOURCE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Bitmap getResource() {
        if (mBitmap == null) {
            try {
                Context context = Hachi.getContext();
                InputStream in = context.getAssets().open(mResourceUrl);
                mBitmap = BitmapFactory.decodeStream(in);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return mBitmap;
    }


}
