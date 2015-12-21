package com.waylens.hachi.snipe.toolbox;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.AccData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;

/**
 * Created by Xiaofei on 2015/12/21.
 */
public class LiveRawDataRequest extends VdbRequest<RawDataItem> {
    private static final String TAG = LiveRawDataRequest.class.getSimpleName();
    private final int mDataType;
    private static int mSharedDataType = 0;

    public LiveRawDataRequest(int method, int dataType, VdbResponse.Listener<RawDataItem> listener,
                              VdbResponse.ErrorListener errorListener) {
        super(method, listener, errorListener);
        this.mDataType = dataType;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        if (mSharedDataType != mDataType) {
            mVdbCommand = VdbCommand.Factory.createCmdSetRawDataOption(mDataType);
            mSharedDataType = mDataType;
        } else {
            mVdbCommand = VdbCommand.Factory.createDummyGetRawData();
        }

        return mVdbCommand;
    }

    @Override
    protected VdbResponse<RawDataItem> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).d("response: " + response.getRetCode());
            return null;
        }

        int dataType = response.readi32();
        byte[] data = response.readByteArray();



        RawDataItem item = new RawDataItem();
        item.dataType = dataType;
        if (item.dataType == RawDataBlock.RAW_DATA_ACC) {
            item.object = AccData.parse(data);
        }


        return VdbResponse.success(item);
    }
}
