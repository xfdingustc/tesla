package com.waylens.hachi.skin;

import org.json.JSONObject;

/**
 * Created by Xiaofei on 2015/9/7.
 */
public abstract class Element {
    public static final String ELEMENT_TYPE_FRAME_SEQUENCE_STR = "FrameSequence";

    public static final int ELEMENT_TYPE_FRAME_SEQUENCE = 0;

    public abstract void parse(JSONObject object);

    public static class ElementFractory {


        public static Element createElement(String type) {
            int elementType = 0;
            if (type.equals(ELEMENT_TYPE_FRAME_SEQUENCE_STR)) {
                elementType = ELEMENT_TYPE_FRAME_SEQUENCE;
            }

            return createElement(elementType);
        }

        public static Element createElement(int type) {
            switch (type) {
                case ELEMENT_TYPE_FRAME_SEQUENCE:
                    return new ElementFrameSequence();
            }

            return null;
        }
    }

}
