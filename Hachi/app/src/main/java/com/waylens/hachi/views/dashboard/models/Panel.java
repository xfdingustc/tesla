package com.waylens.hachi.views.dashboard.models;

import android.graphics.Bitmap;

import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/9/7.
 */
public class Panel extends Element {
    private static final String TAG = Panel.class.getSimpleName();

    private static final String TAG_ELEMENTS = "Elements";

    protected List<Element> mElementList = new ArrayList<>();

    public List<Element> getElementList() {
        return mElementList;
    }

    @Override
    public void parse(JSONObject object) {
        super.parse(object);
        try {
            JSONArray elements = object.getJSONArray(TAG_ELEMENTS);
            for (int i = 0; i < elements.length(); i++) {
                JSONObject elementObj = elements.getJSONObject(i);
                Element element = new Element();
                element.parse(elementObj);
                if (element.mType != ELEMENT_TYPE_UNKNOWN) {
                    mElementList.add(element);
                } else {
                    Logger.t(TAG).d("Unknown element");
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Bitmap getResource() {
        return null;
    }

}