package com.waylens.hachi.hardware.vdtcamera;

/**
 * Created by Xiaofei on 2016/5/30.
 */
class VdtCameraCommand {
    final int mDomain;
    final int mCmd;
    final String mP1;
    final String mP2;

    VdtCameraCommand(int domain, int cmd, String p1, String p2) {
        mDomain = domain;
        mCmd = cmd;
        mP1 = p1;
        mP2 = p2;
    }
}
