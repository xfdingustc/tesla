package com.waylens.hachi.skin;

import com.waylens.hachi.views.ContainerLayouts;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/9/7.
 */
public class Panel extends Element  {
    public static final String PANEL_TYPE_GFORCE_STR = "gforce";
    protected static final int PANEL_TYPE_GFORCE = 0;

    private static final String TAG_WIDTH = "Width";
    private static final String TAG_HEIGHT = "Height";


    protected int mType;
    protected List<Element> mElementList = new ArrayList<>();

    public List<Element> getElementList() {
        return mElementList;
    }


    public static class PanelFactory {
        public static Panel createPanel(String type) {
            int panelType = 0;

            if (type.equals(PANEL_TYPE_GFORCE)) {
                panelType = 0;
            }



            return createPanel(panelType);

        }

        public static Panel createPanel(int type) {
            switch (type) {
                case PANEL_TYPE_GFORCE:
                    return new PanelGforce();

            }

            return null;

        }
    }
}
