package com.waylens.hachi.utils;

import android.text.TextUtils;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.bean.Firmware;

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
            if (!TextUtils.isEmpty(firmware.name)
                && firmware.name.equals(VdtCameraManager.getManager().getCurrentCamera().getHardwareName())) {
                FirmwareVersion versionFromServer = new FirmwareVersion(firmware.version);
                FirmwareVersion versionInCamera = new FirmwareVersion(vdtCamera.getApiVersion());
                if (versionFromServer.isGreaterThan(versionInCamera)) {
                    return firmware;
                }
            }
        }
        return null;
    }


    private static class FirmwareVersion {
        private int mMain;
        private int mSub;
        private int mBuild;

        public FirmwareVersion(String firmware) {
            int main = 0, sub = 0;
            String build = "";
            int i_main = firmware.indexOf('.', 0);
            if (i_main >= 0) {
                String t = firmware.substring(0, i_main);
                main = Integer.parseInt(t);
                i_main++;
                int i_sub = firmware.indexOf('.', i_main);
                if (i_sub >= 0) {
                    t = firmware.substring(i_main, i_sub);
                    sub = Integer.parseInt(t);
                    i_sub++;
                    build = firmware.substring(i_sub);
                }
            }
            mMain = main;
            mSub = sub;
            mBuild = Integer.parseInt(build);
        }


        public boolean isGreaterThan(FirmwareVersion firmwareVersion) {
            if (this.mMain > firmwareVersion.mMain) {
                return true;
            }
            if (this.mSub > firmwareVersion.mSub) {
                return true;
            }

//            return this.mBuild > firmwareVersion.mBuild;
            return true;

        }
    }
}
