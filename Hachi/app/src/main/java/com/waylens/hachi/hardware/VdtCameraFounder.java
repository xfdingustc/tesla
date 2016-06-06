package com.waylens.hachi.hardware;

import android.net.nsd.NsdServiceInfo;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;

/**
 * Created by Xiaofei on 2016/6/6.
 */
public class VdtCameraFounder extends Thread {
    private final static String TAG = VdtCameraFounder.class.getSimpleName();


    @Override
    public void run() {
        super.run();
        while (true) {
            CameraDiscovery.discoverCameras(Hachi.getContext(), new CameraDiscovery.Callback() {
                @Override
                public void onCameraFound(NsdServiceInfo cameraService) {
                    String serviceName = cameraService.getServiceName();
                    boolean bIsPcServer = serviceName.equals("Vidit Studio");
                    final VdtCamera.ServiceInfo serviceInfo = new VdtCamera.ServiceInfo(
                        cameraService.getHost(),
                        cameraService.getPort(),
                        "", serviceName, bIsPcServer);
                    VdtCameraManager.getManager().connectCamera(serviceInfo);

                }

                @Override
                public void onError(int errorCode) {
                    Logger.t(TAG).e("errorCode: " + errorCode);
                }
            });

            try {
                sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            CameraDiscovery.stopDiscovery();
        }
    }
}
