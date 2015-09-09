package com.waylens.hachi.skin;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.ViewGroup;

import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/9/2.
 */
public class Skin {
    private static final String TAG = Skin.class.getSimpleName();

    private static final String TAG_DASHBOARD = "Dashboard";
    private static final String TAG_PANELS = "Panels";
    private static final String TAG_TYPE = "Type";


    private final String mName;

    private List<Panel> mPanels = new ArrayList<>();

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


        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            Logger.t(TAG).e(e.toString());
        }


    }

    public List<Panel> getPanels() {
        return mPanels;
    }



    private void parsePanel(JSONArray panelArray) {
        try {
            for (int i = 0; i < panelArray.length(); i++) {
                JSONObject object = panelArray.getJSONObject(i);
                String type = object.getString(TAG_TYPE);
                Logger.t(TAG).d(type);
                Panel panel = Panel.PanelFactory.createPanel(type);
                panel.parse(object);
                mPanels.add(panel);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void inflate(Canvas canvas) {
       // for (Panel panel : mPanels) {
            //panel.inflate(parent);
        //}
    }

}
