package com.waylens.hachi.camera.connectivity;

import android.net.ConnectivityManager;
import android.net.nsd.NsdServiceInfo;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.utils.ConnectivityHelper;

/**
 * Created by Xiaofei on 2016/10/9.
 */

public class VdtCameraConnectivityManager {
    private static final String TAG = VdtCameraConnectivityManager.class.getSimpleName();
    private DeviceScanner mScanner;
    private static VdtCameraConnectivityManager _INSTANCE = new VdtCameraConnectivityManager();

    public static VdtCameraConnectivityManager getManager() {
        if (_INSTANCE == null) {
            synchronized (VdtCameraConnectivityManager.class) {
                if (_INSTANCE == null) {
                    _INSTANCE = new VdtCameraConnectivityManager();
                }
            }
        }

        return _INSTANCE;
    }

    private VdtCameraConnectivityManager() {

    }

    public void startSearchCamera() {
        ConnectivityHelper.setPreferredNetwork(ConnectivityManager.TYPE_WIFI);
        CameraDiscovery.discoverCameras(Hachi.getContext(), new CameraDiscovery.Callback() {
            @Override
            public void onCameraFound(NsdServiceInfo cameraService) {
                String serviceName = cameraService.getServiceName();
                boolean bIsPcServer = serviceName.equals("Vidit Studio");
                final VdtCamera.ServiceInfo serviceInfo = new VdtCamera.ServiceInfo(
                    cameraService.getHost(),
                    cameraService.getPort(),
                    "", serviceName, bIsPcServer);
                VdtCameraManager.getManager().connectCamera(serviceInfo, "CameraDiscovery");

            }

            @Override
            public void onError(int errorCode) {
                Logger.t(TAG).e("errorCode: " + errorCode);
            }
        });

        startDeviceScanner();
    }

    private void startDeviceScanner() {
        if (mScanner != null) {
            mScanner.stopWork();
        }
        mScanner = new DeviceScanner(Hachi.getContext());
        mScanner.startWork();
    }

    public void stopSearchCamera() {
        CameraDiscovery.stopDiscovery();
        mScanner.stopWork();
    }
}
