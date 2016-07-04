package com.waylens.hachi.hardware.vdtcamera.mina;

import com.waylens.hachi.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/7/4.
 */
public class VdtMessage {
    public final int domain;
    public final int messageType;
    public final String parameter1;
    public final String parameter2;

    public VdtMessage(int domain, int messageType, String parameter1, String parameter2) {
        this.domain = domain;
        this.messageType = messageType;
        this.parameter1 = parameter1;
        this.parameter2 = parameter2;
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
