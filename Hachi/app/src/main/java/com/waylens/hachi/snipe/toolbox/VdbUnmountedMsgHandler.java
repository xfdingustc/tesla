package com.waylens.hachi.snipe.toolbox;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbMessageHandler;
import com.waylens.hachi.snipe.VdbResponse;

/**
 * Created by laina on 16/6/28.
 */
public class VdbUnmountedMsgHandler extends VdbMessageHandler<Object> {
public VdbUnmountedMsgHandler(VdbResponse.Listener<Object> listener,
        VdbResponse.ErrorListener errorListener) {
        super(VdbCommand.Factory.MSG_VdbUnmounted, listener, errorListener);
    }

    @Override
    protected VdbResponse<Object> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            return null;
        }
        Object object = new Object();
        return VdbResponse.success(object);
    }
}
