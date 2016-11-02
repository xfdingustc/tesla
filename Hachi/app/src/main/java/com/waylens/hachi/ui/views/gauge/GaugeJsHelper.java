package com.waylens.hachi.ui.views.gauge;

import com.waylens.hachi.ui.clips.player.GaugeInfoItem;

/**
 * Created by Xiaofei on 2016/11/2.
 */

public class GaugeJsHelper {
    public static String jsInitDefaultGauge() {
        return "javascript:initDefaultGauge()";
    }

    public static String jsSetRotate(boolean rotate) {
        return "javascript:setState({" + "ifRotate:" + rotate + "})";
    }

    public static String jsUpdate() {
        return "javascript:update()";
    }

    public static String jsSetTheme(String theme) {
        if (theme.equals("")) {
            theme = "default";
        }
        return "javascript:setTheme('" + theme + "')";
    }

    public static String jsUpdateGaugeSetting(GaugeInfoItem item) {
        String jsApi = "javascript:setGauge('" + item.title + "',";

        if (!item.isEnabled) {
            jsApi += "'')";

        } else {
            if (item.getOption().equals("large")) {
                jsApi += "'L')";
            } else if (item.getOption().equals("middle")) {
                jsApi += "'M')";
            } else if (item.getOption().equals("small")) {
                jsApi += "'S')";
            }

        }
        return jsApi;
    }
}
