package com.waylens.hachi.ui.fragments.clipplay2;

/**
 * Created by Xiaofei on 2016/3/7.
 */
public class GaugeInfoItem {
    public static final int SIZE_UNKNOWN = -1;
    public static final int SIZE_SMALL = 0;
    public static final int SIZE_MEDIUM = 1;
    public static final int SIZE_LARGE = 2;

    public String title;

    public boolean isEnable;

    public int sizeType;

    public GaugeInfoItem(String title, int defaultSizeType) {
        this.title = title;
        this.sizeType = defaultSizeType;
        this.isEnable = true;
    }
}
