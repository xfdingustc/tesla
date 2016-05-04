package com.waylens.hachi.app;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.clips.clipplay2.GaugeInfoItem;
import com.waylens.hachi.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

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
}
