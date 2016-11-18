package com.waylens.hachi.utils;

import android.text.TextUtils;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.bean.Firmware;
import com.waylens.hachi.snipe.utils.ToStringUtils;

import java.util.List;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by Xiaofei on 2016/10/13.
 */

public class FirmwareUpgradeHelper {
    private static final String TAG = FirmwareUpgradeHelper.class.getSimpleName();

    public static Observable<Firmware> getNewerFirmwareRx() {
        return HachiService.createHachiApiService().getFirmwareRx()
            .map(new Func1<List<Firmware>, Firmware>() {
                @Override
                public Firmware call(List<Firmware> firmwares) {
                    return getNewerFireware(firmwares);
                }
            });
    }

    private static Firmware getNewerFireware(List<Firmware> firmwares) {
        VdtCamera vdtCamera = VdtCameraManager.getManager().getCurrentCamera();
        if (vdtCamera == null) {
            return null;
        }
        for (int i = 0; i < firmwares.size(); i++) {
            final Firmware firmware = firmwares.get(i);
            Logger.t(TAG).d("one firmware: " + firmware.toString());
            if (!TextUtils.isEmpty(firmware.name)
                && firmware.name.equals(VdtCameraManager.getManager().getCurrentCamera().getHardwareName())) {
                FirmwareVersion versionFromServer = new FirmwareVersion(firmware.version);
                FirmwareVersion versionInCamera = new FirmwareVersion(vdtCamera.getApiVersion());
                Logger.t(TAG).d("latest version: " + versionFromServer);
                Logger.t(TAG).d("version of camera: " + versionInCamera);
                if (versionFromServer.isGreaterThan(versionInCamera)) {
                    return firmware;
                }
            }
        }
        return null;
    }


    private static class FirmwareVersion {
        private String mMain;
        private String mSub;
        private String mBuild;

        public FirmwareVersion(String firmware) {
            int i_main = firmware.indexOf('.', 0);
            if (i_main >= 0) {
                mMain = firmware.substring(0, i_main);
                i_main++;
                int i_sub = firmware.indexOf('.', i_main);
                if (i_sub >= 0) {
                    mSub = firmware.substring(i_main, i_sub);
                    i_sub++;
                    mBuild = firmware.substring(i_sub);
                }
            }
        }


        public boolean isGreaterThan(FirmwareVersion firmwareVersion) {
            return this.toInteger() > firmwareVersion.toInteger();
        }

        @Override
        public String toString() {
            return mMain + mSub + mBuild;
        }

        public int toInteger() {
            return Integer.parseInt(mMain + mSub + mBuild);
        }
    }
}
