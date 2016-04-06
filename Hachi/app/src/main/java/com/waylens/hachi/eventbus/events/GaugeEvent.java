package com.waylens.hachi.eventbus.events;

import com.waylens.hachi.ui.fragments.clipplay2.GaugeInfoItem;

/**
 * Created by Xiaofei on 2016/4/6.
 */
public class GaugeEvent {
    private final GaugeInfoItem mGaugeInfoItem;

    public GaugeEvent(GaugeInfoItem item) {
        this.mGaugeInfoItem = item;
    }

    public GaugeInfoItem getGaugeInfoItem() {
        return mGaugeInfoItem;
    }
}
