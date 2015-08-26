package com.waylens.hachi.snipe.events;

import com.waylens.hachi.snipe.VdbAcknowledge;

/**
 * Created by Xiaofei on 2015/8/25.
 */
public class GetIndexPictureEvent extends Event {
    public GetIndexPictureEvent(VdbAcknowledge acknowledge) {
        super(acknowledge);
    }
}
