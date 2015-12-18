package com.waylens.hachi.views.dashboard.models;

import android.graphics.Bitmap;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Xiaofei on 2015/9/7.
 */
public abstract class Element {
    public static final int ELEMENT_TYPE_FRAME_SEQUENCE = 0;
    public static final int ELEMENT_TYPE_STATIC_IMAGE = 1;
    public static final int ELEMENT_TYPE_PROGRESS_IMAGE = 2;
    public static final int ElEMENT_TYPE_ROTATE_PROGRESS_IMAGE = 3;
    public static final int ELEMENT_TYPE_NUMBER_VIEW = 4;
    public static final int ELEMENT_TYPE_MAP = 5;

    private static final String ELEMENT_TYPE_FRAME_SEQUENCE_STR = "FrameSequence";
    private static final String ELEMENT_TYPE_STATIC_IMAGE_STR = "StaticImage";
    private static final String ELEMENT_TYPE_PROGRESS_IMAGE_STR = "ProgressImage";
    private static final String ELEMENT_TYPE_ROTATE_PROGRESS_IMAGE_STR = "RotateProgressImage";
    private static final String ELEMENT_TYPE_NUMBER_VIEW_STR = "NumberView";
    private static final String ELEMENT_TYPE_MAP_STR = "Map";

    private static final String TAG_WIDTH = "Width";
    private static final String TAG_HEIGHT = "Height";
    private static final String TAG_MARGIN_TOP = "MarginTop";
    private static final String TAG_MARGIN_BOTTOM = "MarginBottom";
    private static final String TAG_MARGIN_LEFT = "MarginLeft";
    private static final String TAG_MARGIN_RIGHT = "MarginRight";
    private static final String TAG_ALIGNMENT = "Alignment";
    private static final String TAG_ROTATION = "Rotation";
    private static final String TAG_SUBSCRIBE = "Subscribe";
    private static final String TAG_XCOORD = "XCoord";
    private static final String TAG_YCOORD = "YCoord";

    public static final String MATCH_PARENT = "MatchParent";
    public static final String WRAP_CONTENT = "WrapContent";

    public static final int SIZE_MODE_MATCH_PARENT = 0;
    public static final int SIZE_MODE_WRAP_CONTENT = 1;
    public static final int SIZE_MODE_FIXED = 2;

    public static final int TOP_LEFT = 1;
    public static final int TOP_CENTER = 2;
    public static final int TOP_RIGHT = 3;
    public static final int CENTER_LEFT = 4;
    public static final int CENTER = 5;
    public static final int CENTER_RIGHT = 6;
    public static final int BOTTOM_LEFT = 7;
    public static final int BOTTOM_CENTER = 8;
    public static final int BOTTOM_RIGHT = 9;

    protected int mType;
    private int mWidth;
    private int mHeight;
    private int mWidthSizeMode;
    private int mHeightSizeMode;
    private int mMarginTop = 0;
    private int mMarginBottom = 0;
    private int mMarginLeft = 0;
    private int mMarginRight = 0;
    private int mAlignment;
    private int mRotation = 0;
    private float mXCoord = 0;
    private float mYCoord = 0;

    private String mSubscribe = null;
    protected String mResourceUrl;

    protected Map<String, String> mAttributeSet = new HashMap<>();

    public int getType() {
        return mType;
    }

    public String getAttribute(String name) {
        return mAttributeSet.get(name);
    }

    public int getWidthSizeMode() {
        return mWidthSizeMode;
    }

    public int getHeightSizeMode() {
        return mHeightSizeMode;
    }

    public int getWidth() {
        if (mWidth != 0 || getResource() == null) {
            return mWidth;
        } else {
            return getResource().getWidth();
        }
    }

    public int getHeight() {
        if (mHeight != 0 || getResource() == null) {
            return mHeight;
        } else {
            return getResource().getHeight();
        }
    }


    public int getAlignment() {
        return mAlignment;
    }

    public int getMarginTop() {
        return mMarginTop;
    }

    public int getMarginBottom() {
        return mMarginBottom;
    }

    public int getMarginLeft() {
        return mMarginLeft;
    }

    public int getMarginRight() {
        return mMarginRight;
    }

    public int getRotation() {
        return mRotation;
    }

    public float getXCoord() {
        return mXCoord;
    }

    public float getYCoord() {
        return mYCoord;
    }

    public String getSubscribe() {
        return mSubscribe;
    }


    public void parse(JSONObject object) {
        // Get width & widthMode
        String width = object.optString(TAG_WIDTH, null);

        if (width == null || width.equals(WRAP_CONTENT)) {
            mWidthSizeMode = SIZE_MODE_WRAP_CONTENT;
        } else if (width.equals(MATCH_PARENT)) {
            mWidthSizeMode = SIZE_MODE_MATCH_PARENT;
        } else {
            mWidthSizeMode = SIZE_MODE_FIXED;
            mWidth = Integer.parseInt(width);
        }

        // Get height & heightMode
        String height = object.optString(TAG_HEIGHT, null);
        if (height == null || height.equals(WRAP_CONTENT)) {
            mHeightSizeMode = SIZE_MODE_WRAP_CONTENT;
        } else if (height.equals(MATCH_PARENT)) {
            mHeightSizeMode = SIZE_MODE_MATCH_PARENT;
        } else {
            mHeightSizeMode = SIZE_MODE_FIXED;
            mHeight = Integer.parseInt(height);
        }

        String alignment = object.optString(TAG_ALIGNMENT);
        mAlignment = getAlignment(alignment);
        mMarginTop = object.optInt(TAG_MARGIN_TOP);
        mMarginBottom = object.optInt(TAG_MARGIN_BOTTOM);
        mMarginLeft = object.optInt(TAG_MARGIN_LEFT);
        mMarginRight = object.optInt(TAG_MARGIN_RIGHT);
        mRotation = object.optInt(TAG_ROTATION);
        mXCoord = (float) object.optDouble(TAG_XCOORD);
        mYCoord = (float) object.optDouble(TAG_YCOORD);
        mSubscribe = object.optString(TAG_SUBSCRIBE);


    }



    private int getAlignment(String alignment) {
        int ret = 0;
        if (alignment.equals("Bottom_Left")) {
            ret = BOTTOM_LEFT;
        } else if (alignment.equals("Center")) {
            ret = CENTER;
        } else if (alignment.equals("Top_Left")) {
            ret = TOP_LEFT;
        } else if (alignment.equals("Top_Center")) {
            ret = TOP_CENTER;
        } else if (alignment.equals("Bottom_Center")) {
            ret = BOTTOM_CENTER;
        } else if (alignment.equals("Top_Right")) {
            ret = TOP_RIGHT;
        } else if (alignment.equals("Center_Right")) {
            ret = CENTER_RIGHT;
        } else if (alignment.equals("Center_Left")) {
            ret = CENTER_LEFT;
        } else if (alignment.equals("Bottom_Right")) {
            ret = BOTTOM_RIGHT;
        }
        return ret;
    }

    public abstract Bitmap getResource();

    public static class ElementFractory {
        public static Element createElement(String type) {
            int elementType = -1;
            if (type.equals(ELEMENT_TYPE_FRAME_SEQUENCE_STR)) {
                elementType = ELEMENT_TYPE_FRAME_SEQUENCE;
            } else if (type.equals(ELEMENT_TYPE_STATIC_IMAGE_STR)) {
                elementType = ELEMENT_TYPE_STATIC_IMAGE;
            } else if (type.equals(ELEMENT_TYPE_PROGRESS_IMAGE_STR)) {
                elementType = ELEMENT_TYPE_PROGRESS_IMAGE;
            } else if (type.equals(ELEMENT_TYPE_ROTATE_PROGRESS_IMAGE_STR)) {
                elementType = ElEMENT_TYPE_ROTATE_PROGRESS_IMAGE;
            } else if (type.equals(ELEMENT_TYPE_NUMBER_VIEW_STR)) {
                elementType = ELEMENT_TYPE_NUMBER_VIEW;
            } else if (type.equals(ELEMENT_TYPE_MAP_STR)) {
                elementType = ELEMENT_TYPE_MAP;
            }

            return createElement(elementType);
        }

        public static Element createElement(int type) {
            switch (type) {
                case ELEMENT_TYPE_FRAME_SEQUENCE:
                    return new ElementFrameSequence();
                case ELEMENT_TYPE_STATIC_IMAGE:
                    return new ElementStaticImage();
                case ELEMENT_TYPE_PROGRESS_IMAGE:
                    return new ElementProgressImage();
                case ElEMENT_TYPE_ROTATE_PROGRESS_IMAGE:
                    return new ElementRotateProgressImage();
                case ELEMENT_TYPE_NUMBER_VIEW:
                    return new ElementNumber();
                case ELEMENT_TYPE_MAP:
                    return new ElementMap();
            }

            return null;
        }
    }

}
