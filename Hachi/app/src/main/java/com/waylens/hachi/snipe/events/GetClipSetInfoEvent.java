package com.waylens.hachi.snipe.events;

import com.waylens.hachi.snipe.VdbAcknowledge;

/**
 * Created by Xiaofei on 2015/8/25.
 */
public class GetClipSetInfoEvent extends Event {
    public GetClipSetInfoEvent(VdbAcknowledge acknowledge) {
        super(acknowledge);
    }
}
