package com.waylens.hachi.ui.clips.clipplay2;

/**
 * Created by Xiaofei on 2016/3/7.
 */
public class GaugeInfoItem {

    public enum Option {NA, large, middle, small}

    public static final GaugeInfoItem.Option[] DEFAULT_OPTIONS = new GaugeInfoItem.Option[]{
            GaugeInfoItem.Option.large,
            GaugeInfoItem.Option.middle,
            GaugeInfoItem.Option.small,
            GaugeInfoItem.Option.small,
            GaugeInfoItem.Option.small,
            GaugeInfoItem.Option.large,
            GaugeInfoItem.Option.middle,
            GaugeInfoItem.Option.large,
    };



    //NOTE: Keep the same order as R.array.supported_gauges
    public static final String[] OPTION_JS_PARAMS = new String[]{
            "showSpeedThrottle",
            "showRpm",
            "showAmbient",
            "showPsi",
            "showTimeDate",
            "showGforce",
            "showRollPitch",
            "showGps",
    };

    private final int id;

    public final String title;

    public String option;

    public boolean isEnabled;

    public GaugeInfoItem(int id, String title, String option) {
        this.id = id;
        this.title = title;
        this.option = option;
        this.isEnabled = true;
    }

    public String getJSParam() {
        if (id >= 0 && id < OPTION_JS_PARAMS.length) {
            return OPTION_JS_PARAMS[id];
        } else {
            return "invalid";
        }
    }

    public String getOption() {
//        if (isEnabled) {
//            return option.name();
//        } else {
//            return Option.NA.name();
//        }
        return option;
    }
}
