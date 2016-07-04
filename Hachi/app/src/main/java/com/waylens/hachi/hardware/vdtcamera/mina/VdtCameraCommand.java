package com.waylens.hachi.hardware.vdtcamera.mina;

/**
 * Created by Xiaofei on 2016/7/4.
 */
public class VdtCameraCommand {
    public final int mDomain;
    public final int mCmd;
    public final String mP1;
    public final String mP2;

    public VdtCameraCommand(int domain, int cmd, String p1, String p2) {
        mDomain = domain;
        mCmd = cmd;
        mP1 = p1;
        mP2 = p2;
    }
}
