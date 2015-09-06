package com.waylens.hachi.skin;

import android.content.Context;
import android.util.Xml;

import com.orhanobut.logger.Logger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Xiaofei on 2015/9/2.
 */
public class Skin {
    private static final String TAG = Skin.class.getSimpleName();

    private static final String TAG_PANEL = "Panel";

    private static final String VALUE_TYPE = "type";

    private final String mName;

    public Skin(String name) {
        this.mName = name;
    }

    public void load(Context context, String asset) {
        try {
            InputStream in  = context.getAssets().open(asset);
            XmlPullParser parser = Xml.newPullParser();

            parser.setInput(in, "UTF-8");

            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        Logger.t(TAG).d(parser.getName());
                        if (parser.getName().equals(TAG_PANEL)) {
                            parsePanel(parser);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    private void parsePanel(XmlPullParser parser) {

    }
}
