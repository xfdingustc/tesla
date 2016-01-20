package com.waylens.hachi.ui.views.dashboard.models;

import android.content.Context;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/9/2.
 */
public class SkinManager {
    private static final String TAG = SkinManager.class.getSimpleName();

    private static final String SKIN_TAG = "skin";
    private static final String VALUE_NAME = "name";
    private static final String VALUE_ASSET = "asset";

    private static SkinManager mSharedManager = null;
    private static Context mSharedContext;

    private List<Skin> mSkinList = new ArrayList<>();

    private SkinManager() {

    }

    public static SkinManager getManager() {
        if (mSharedManager == null) {
            mSharedManager = new SkinManager();
        }

        return mSharedManager;
    }

    public static void initialize(Context context) {
        mSharedContext = context;
    }


    public void load() {
        try {
            InputStream in = mSharedContext.getAssets().open("skins.xml");
            parseSkins(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void parseSkins(InputStream in) {
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(in, "UTF-8");
            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if (parser.getName().equals(SKIN_TAG)) {
                            Skin skin = new Skin(parser.getAttributeValue(null, VALUE_NAME));
                            skin.load(mSharedContext, parser.getAttributeValue(null, VALUE_ASSET));
                            mSkinList.add(skin);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Skin getSkin() {
        if (mSkinList.size() > 0) {
            return mSkinList.get(0);
        }

        return null;
    }
}
