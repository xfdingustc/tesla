package com.waylens.hachi.snipe.toolbox;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.RawDataItem;

/**
 * Created by Xiaofei on 2015/12/21.
 */
public class LiveRawDataRequest extends VdbRequest<RawDataItem> {

    public LiveRawDataRequest(int method, VdbResponse.Listener<RawDataItem> listener, VdbResponse.ErrorListener errorListener) {
        super(method, listener, errorListener);
    }

    @Override
    protected VdbCommand createVdbCommand() {
        return null;
    }

    @Override
    protected VdbResponse<RawDataItem> parseVdbResponse(VdbAcknowledge response) {
        return null;
    }
}
