package com.waylens.hachi.views.dashboard.models;

import android.content.Context;

import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by Xiaofei on 2015/9/2.
 */
public class Skin {
    private static final String TAG = Skin.class.getSimpleName();

    private static final String TAG_DASHBOARD = "Dashboard";
    private static final String TAG_FONTS = "Fonts";
    private static final String TAG_FONT_NAME = "FontName";
    private static final String TAG_PANELS = "Panels";
    private static final String TAG_TYPE = "Type";


    private final String mName;

    private List<Panel> mPanels = new ArrayList<>();
    private Hashtable<String, Font> mFonts = new Hashtable<>();

    public Skin(String name) {
        this.mName = name;
    }

    public void load(Context context, String asset) {
        try {
            InputStream in = context.getAssets().open(asset);
            byte[] buffer = new byte[in.available()];
            in.read(buffer);

            String jsonString = new String(buffer);

            JSONObject skin = new JSONObject(jsonString);
            JSONObject dashboard = skin.getJSONObject(TAG_DASHBOARD);

            parsePanel(dashboard.getJSONArray(TAG_PANELS));
            parseFonts(dashboard.getJSONArray(TAG_FONTS));


        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            Logger.t(TAG).e(e.toString());
        }


    }

    public List<Panel> getPanels() {
        return mPanels;
    }

    public Font getFont(String fontName) {
        return mFonts.get(fontName);
    }


    private void parsePanel(JSONArray panelArray) {
        try {
            for (int i = 0; i < panelArray.length(); i++) {
                JSONObject object = panelArray.getJSONObject(i);
                Panel panel = new Panel();
                panel.parse(object);
                mPanels.add(panel);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseFonts(JSONArray fontsArray) {
        try {
            for (int i = 0; i < fontsArray.length(); i++) {
                JSONObject fontObj = fontsArray.getJSONObject(i);
                String name = fontObj.getString(TAG_FONT_NAME);
                Font font = new Font();
                font.parse(fontObj);
                mFonts.put(name, font);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}
