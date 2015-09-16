package com.waylens.hachi.skin;

import org.json.JSONObject;

/**
 * Created by Xiaofei on 2015/9/16.
 */
public class ElementRotateProgressImage extends ElementProgressImage {
    private static final String TAG_START_ANGLE = "StartAngle";
    private static final String TAG_END_ANGLE = "EndAngle";

    private float mStartAngle;
    private float mEndAngle;

    public ElementRotateProgressImage() {
        mType = ElEMENT_TYPE_ROTATE_PROGRESS_IMAGE;
    }


    public float getStartAngle() {
        return mStartAngle;
    }

    public float getEndAngle() {
        return mEndAngle;
    }


    @Override
    public void parse(JSONObject object) {
        super.parse(object);
        mStartAngle = (float)object.optDouble(TAG_START_ANGLE);
        mEndAngle = (float)object.optDouble(TAG_END_ANGLE);
    }
}
