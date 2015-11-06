package com.waylens.hachi.skin;

import android.content.Context;
import android.graphics.Bitmap;

import com.waylens.hachi.views.ContainerLayouts;

import org.json.JSONObject;

/**
 * Created by Xiaofei on 2015/9/7.
 */
public abstract class Element implements ContainerLayouts {
    public static final String ELEMENT_TYPE_FRAME_SEQUENCE_STR = "FrameSequence";
    public static final String ELEMENT_TYPE_STATIC_IMAGE_STR = "StaticImage";
    public static final String ELEMENT_TYPE_PROGRESS_IMAGE_STR = "ProgressImage";
    public static final String ELEMENT_TYPE_ROTATE_PROGRESS_IMAGE_STR = "RotateProgressImage";
    public static final String ELEMENT_TYPE_NUMBER_VIEW_STR = "NumberView";

    public static final int ELEMENT_TYPE_FRAME_SEQUENCE = 0;
    public static final int ELEMENT_TYPE_STATIC_IMAGE = 1;
    public static final int ELEMENT_TYPE_PROGRESS_IMAGE = 2;
    public static final int ElEMENT_TYPE_ROTATE_PROGRESS_IMAGE = 3;
    public static final int ELEMENT_TYPE_NUMBER_VIEW = 4;

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

    protected int mType;
    private int mWidth;
    private int mHeight;
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

    public int getType() {
        return mType;
    }

    public int getWidth() {
        if (mWidth != 0) {
            return mWidth;
        } else {
            return getResource().getWidth();
        }
    }

    public int getHeight() {
        if (mHeight != 0) {
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
        mWidth = object.optInt(TAG_WIDTH);
        mHeight = object.optInt(TAG_HEIGHT);
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
            int elementType = 0;
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
            }

            return null;
        }
    }

}