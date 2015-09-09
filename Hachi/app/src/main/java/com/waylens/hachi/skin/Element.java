package com.waylens.hachi.skin;

import android.content.Context;
import android.graphics.Bitmap;

import com.waylens.hachi.views.ContainerLayouts;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Xiaofei on 2015/9/7.
 */
public class Element implements ContainerLayouts {
    public static final String ELEMENT_TYPE_FRAME_SEQUENCE_STR = "FrameSequence";
    public static final String ELEMENT_TYPE_STATIC_IMAGE_STR = "StaticImage";

    private static final String TAG_WIDTH = "Width";
    private static final String TAG_HEIGHT = "Height";
    private static final String TAG_ALIGNMENT = "Alignment";

    public static final int ELEMENT_TYPE_FRAME_SEQUENCE = 0;
    public static final int ELEMENT_TYPE_STATIC_IMAGE = 1;

    protected String mResourceUrl;

    private int mWidth;
    private int mHeight;
    private int mAlignment;

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }
    public int getAlignment() {
        return mAlignment;
    }

    public void parse(JSONObject object) {
        try {
            mWidth = object.getInt(TAG_WIDTH);
            mHeight = object.getInt(TAG_HEIGHT);
            String alignment = object.optString(TAG_ALIGNMENT);
            mAlignment = getAlignment(alignment);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private int getAlignment(String alignment) {
        int ret = 0;
        if (alignment.equals("Bottom_Left")) {
            ret = BOTTOM_LEFT;
        } else if (alignment.equals("Center")) {
            ret = CENTER;
        }
        return ret;
    }

    public Bitmap getResource(Context context) {
        return null;
    }

    public static class ElementFractory {
        public static Element createElement(String type) {
            int elementType = 0;
            if (type.equals(ELEMENT_TYPE_FRAME_SEQUENCE_STR)) {
                elementType = ELEMENT_TYPE_FRAME_SEQUENCE;
            } else if (type.equals(ELEMENT_TYPE_STATIC_IMAGE_STR)) {
                elementType = ELEMENT_TYPE_STATIC_IMAGE;
            }

            return createElement(elementType);
        }

        public static Element createElement(int type) {
            switch (type) {
                case ELEMENT_TYPE_FRAME_SEQUENCE:
                    return new ElementFrameSequence();
                case ELEMENT_TYPE_STATIC_IMAGE:
                    return new ElementStaticImage();
            }

            return null;
        }
    }

}
