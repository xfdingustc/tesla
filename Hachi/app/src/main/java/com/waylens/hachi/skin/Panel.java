package com.waylens.hachi.skin;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/9/7.
 */
public abstract class Panel {
    public static final String PANEL_TYPE_GFORCE_STR = "gforce";
    protected static final int PANEL_TYPE_GFORCE = 0;

    protected int mType;

    protected List<Element> mElementList = new ArrayList<>();

    abstract public void parse(JSONObject object);

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
