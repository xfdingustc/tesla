package com.waylens.hachi.views.dashboard.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.waylens.hachi.app.Hachi;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Xiaofei on 2015/9/7.
 */
public class Element {
    public static final int ELEMENT_TYPE_UNKNOWN = 0;
    public static final int ELEMENT_TYPE_STATIC_IMAGE = 1;
    public static final int ELEMENT_TYPE_PROGRESS_IMAGE = 2;
    public static final int ElEMENT_TYPE_ROTATE_PROGRESS_IMAGE = 3;
    public static final int ELEMENT_TYPE_NUMBER_VIEW = 4;
    public static final int ELEMENT_TYPE_MAP = 5;
    public static final int ELEMENT_TYPE_STRING = 6;


    private static final String ELEMENT_TYPE_STATIC_IMAGE_STR = "StaticImage";
    private static final String ELEMENT_TYPE_PROGRESS_IMAGE_STR = "ProgressImage";
    private static final String ELEMENT_TYPE_MAP_STR = "Map";
    private static final String ELEMENT_TYPE_STRING_STR = "StringView";

    private static final String TAG_TYPE = "Type";
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
    private static final String TAG_RESOURCE = "Resource";

    public static final String ATTRIBUTE_MAX = "Max";
    public static final String ATTRIBUTE_STYLE = "Style";
    public static final String ATTRIBUTE_DIRECTION = "Direction";
    public static final String ATTRIBUTE_START_RADIUS = "StartRadius";
    public static final String ATTRIBUTE_END_RADIUS = "EndRadius";
    public static final String ATTRIBUTE_FONT = "Font";
    public static final String ATTRIBUTE_START_ANGLE = "StartAngle";
    public static final String ATTRIBUTE_END_ANGLE = "EndAngle";

    private Bitmap mBitmap = null;

    private String[] mSupportedAttributes = {
        ATTRIBUTE_MAX,
        ATTRIBUTE_STYLE,
        ATTRIBUTE_DIRECTION,
        ATTRIBUTE_START_RADIUS,
        ATTRIBUTE_END_RADIUS,
        ATTRIBUTE_FONT,
        ATTRIBUTE_START_ANGLE,
        ATTRIBUTE_END_ANGLE
    };

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

        // Get Type;
        String type = object.optString(TAG_TYPE);
        mType = getElementType(type);


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

        mResourceUrl = object.optString(TAG_RESOURCE, null);
        if (mResourceUrl != null) {

        }

        parseSupportAttributes(object);

    }



    private void parseSupportAttributes(JSONObject object) {
        for (String attribute : mSupportedAttributes) {
            String value = object.optString(attribute);
            if (value != null) {
                mAttributeSet.put(attribute, value);
            }
        }
    }


    private int getElementType(String type) {
        if (type.equals(ELEMENT_TYPE_STATIC_IMAGE_STR)) {
            return ELEMENT_TYPE_STATIC_IMAGE;
        } else if (type.equals(ELEMENT_TYPE_PROGRESS_IMAGE_STR)) {
            return ELEMENT_TYPE_PROGRESS_IMAGE;
        } else if (type.equals(ELEMENT_TYPE_MAP_STR)) {
            return ELEMENT_TYPE_MAP;
        } else if (type.equals(ELEMENT_TYPE_STRING_STR)) {
            return ELEMENT_TYPE_STRING;
        }
        return 0;
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
