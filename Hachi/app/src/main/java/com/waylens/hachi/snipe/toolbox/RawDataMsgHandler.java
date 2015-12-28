package com.waylens.hachi.snipe.toolbox;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbMessageHandler;
import com.waylens.hachi.snipe.VdbResponse;
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
        return VdbResponse.success(new RawDataItem());
    }
}
