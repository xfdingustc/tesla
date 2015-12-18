package com.waylens.hachi.views.dashboard.adapters;

import com.waylens.hachi.vdb.RawDataItem;

/**
 * Created by Xiaofei on 2015/12/18.
 */
public interface IRawDataAdapter {
    RawDataItem getAccDataItem(long pts);

    RawDataItem getObdDataItem(long pts);

    RawDataItem getGpsDataItem(long pts);
}
