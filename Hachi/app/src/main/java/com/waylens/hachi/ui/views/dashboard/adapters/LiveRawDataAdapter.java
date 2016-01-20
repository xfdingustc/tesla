package com.waylens.hachi.ui.views.dashboard.adapters;

import android.util.Log;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.LiveRawDataRequest;
import com.waylens.hachi.snipe.toolbox.RawDataMsgHandler;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;


/**
 * Created by Xiaofei on 2015/12/18.
 */
public class LiveRawDataAdapter extends RawDataAdapter {
    private static final String TAG = LiveRawDataAdapter.class.getSimpleName();

    private final VdbRequestQueue mVdbRequestQueue;
    private RawDataItem mLatestAccRawData = null;

    public LiveRawDataAdapter() {
        this.mVdbRequestQueue = Snipe.newRequestQueue();
        init();
    }

    private void init() {
        LiveRawDataRequest request = new LiveRawDataRequest(RawDataBlock.F_RAW_DATA_GPS +
            RawDataBlock.F_RAW_DATA_ACC + RawDataBlock.F_RAW_DATA_ODB, new
            VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    Logger.t(TAG).d("LiveRawDataResponse: " + response);
                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e(TAG, "LiveRawDataResponse ERROR", error);
            }
        });

        mVdbRequestQueue.add(request);
        RawDataMsgHandler rawDataMsgHandler = new RawDataMsgHandler(new VdbResponse.Listener<RawDataItem>() {
            @Override
            public void onResponse(RawDataItem response) {
//                Logger.t(TAG).d(String.format("RawDataMsgHandler: Type[%d]:[%s]", response
//                    .dataType, response.data));

                notifyDataSetChanged(response);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e(TAG, "RawDataMsgHandler ERROR", error);
            }
        });
        mVdbRequestQueue.registerMessageHandler(rawDataMsgHandler);
    }




}
