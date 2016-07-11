package com.waylens.hachi.app;

import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.clips.player.GaugeInfoItem;
import com.waylens.hachi.utils.PreferenceUtils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Xiaofei on 2016/5/4.
 */
public class GaugeSettingManager  {
    private static GaugeSettingManager mSharedManager = new GaugeSettingManager();

    public static GaugeSettingManager getManager() {
        return mSharedManager;
    }

    public static final String[] DEFAULT_OPTIONS_STR = new String[]{
        "large",
        "middle",
        "small",
        "small",
        "small",
        "large",
        "middle",
        "large",
    };

    public void saveSetting(GaugeInfoItem item) {
        PreferenceUtils.putString(item.title, item.getOption());
    }

    public void saveTheme(String theme) {
        PreferenceUtils.putString("theme", theme);
    }

    public List<GaugeInfoItem> getSetting() {
        List<GaugeInfoItem> itemList = new ArrayList<>();
        String[] supportedGauges = Hachi.getContext().getResources().getStringArray(R.array.supported_gauges);

        int i = 0;
        for (String title : supportedGauges) {
            String option = PreferenceUtils.getString(title, DEFAULT_OPTIONS_STR[i]);
            GaugeInfoItem item = new GaugeInfoItem(i, title, option);
            i++;
            itemList.add(item);
        }


        return itemList;
    }

    public String getTheme() {
        return PreferenceUtils.getString("theme", "default");
    }

    public Map<String, String> getGaugeSettingMap() {
        Map<String, String> gaugeSettingMap = new HashMap<String, String>();
        gaugeSettingMap.put("theme", this.getTheme());
        for (GaugeInfoItem gaugeInfoItem : this.getSetting()) {
            String option = gaugeInfoItem.getOption();
            String value;
            if (!gaugeInfoItem.isEnabled) {
                value = "";
            } else {
                value = gaugeInfoItem.getOption().toUpperCase().substring(0, 1);

            }
            gaugeSettingMap.put(gaugeInfoItem.getJSParam(), value);
            Logger.d(gaugeInfoItem.getJSParam() + ":" + value);
        }
        return gaugeSettingMap;
    }
}
