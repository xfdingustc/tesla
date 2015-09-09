package com.waylens.hachi.skin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Xiaofei on 2015/9/7.
 */
public class PanelGforce extends Panel {
    private static final String TAG_TYPE = "Type";
    private static final String TAG_ELEMENTS = "Elements";



    @Override
    public void parse(JSONObject object) {
        super.parse(object);
        try {
            JSONArray elements = object.getJSONArray(TAG_ELEMENTS);
            for (int i = 0; i < elements.length(); i++) {
                JSONObject elementObj = elements.getJSONObject(i);
                Element element = Element.ElementFractory.createElement(elementObj.getString
                    (TAG_TYPE));
                element.parse(elementObj);
                mElementList.add(element);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
