package com.waylens.hachi.snipe.toolbox;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbMessageHandler;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.AccData;
import com.waylens.hachi.vdb.GPSRawData;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;

/**
 * Created by Richard on 12/28/15.
 */
public class RawDataMsgHandler extends VdbMessageHandler<RawDataItem> {
    public RawDataMsgHandler(VdbResponse.Listener<RawDataItem> listener,
                             VdbResponse.ErrorListener errorListener) {
        super(VdbCommand.Factory.MSG_RawData, listener, errorListener);
    }

    @Override
    protected VdbResponse<RawDataItem> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            return null;
        }
        int dataType = response.readi32();
        byte[] data = response.readByteArray();
        RawDataItem rawDataItem = new RawDataItem();
        rawDataItem.dataType = dataType;
        switch (dataType) {
            case RawDataBlock.RAW_DATA_ODB:
                rawDataItem.object = OBDData.parse(data);
                break;
            case RawDataBlock.RAW_DATA_ACC:
                rawDataItem.object = AccData.parse(data);
                break;
            case RawDataBlock.RAW_DATA_GPS:
                rawDataItem.object = GPSRawData.translate(data);
                break;
            default:
                return null;
        }
        return VdbResponse.success(rawDataItem);
    }
}
