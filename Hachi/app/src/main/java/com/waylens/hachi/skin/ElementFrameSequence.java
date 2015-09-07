package com.waylens.hachi.skin;

import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/9/7.
 */
public class ElementFrameSequence extends Element {
    private static final String TAG = ElementFrameSequence.class.getSimpleName();
    private static final String TAG_RESOURCES = "Resources";
    private static final String TAG_FRAME = "frame";

    private List<String> mImageList = new ArrayList<>();

    @Override
    public void parse(JSONObject object) {
        try {
            JSONArray resources = object.getJSONArray(TAG_RESOURCES);
            for (int i = 0; i < resources.length(); i++) {
                String frame = resources.getJSONObject(i).getString(TAG_FRAME);
                Logger.t(TAG).d("add one frame: " + frame);
                mImageList.add(frame);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
