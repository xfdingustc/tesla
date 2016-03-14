package com.waylens.hachi.app;

import android.app.Application;
import android.content.Context;
import android.net.nsd.NsdServiceInfo;


import com.facebook.FacebookSdk;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.hardware.CameraDiscovery;
import com.waylens.hachi.hardware.DeviceScanner;
import com.waylens.hachi.hardware.NanoMdns;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;

import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.utils.PreferenceUtils;

import im.fir.sdk.FIR;


/**
 * Created by Xiaofei on 2015/8/4.
 */
public class Hachi extends Application {
    private static final String TAG = Hachi.class.getSimpleName();


    private static Context mSharedContext = null;

    private DeviceScanner mScanner;
    private NanoMdns mNanoMdns;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mScanner.stopWork();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //MultiDex.install(this);
    }

    private void init() {
        mSharedContext = getApplicationContext();

        initLogger();

        PreferenceUtils.initialize(this);

        SessionManager.initialize(this);
        SessionManager.getInstance().reloadLoginInfo();

        initCameraManager();

        initFacebookSDK();
        ImageUtils.initImageLoader(this);
        VdbImageLoader.getImageLoader(null).init(this, 1024 * 64);
//        FIR.init(this);

        Snipe.init();



//        CameraDiscovery.discoverCameras(this, new CameraDiscovery.Callback() {
//            @Override
//            public void onCameraFound(NsdServiceInfo cameraService) {
//                String serviceName = cameraService.getServiceName();
//                boolean bIsPcServer = serviceName.equals("Vidit Studio");
//                final VdtCamera.ServiceInfo serviceInfo = new VdtCamera.ServiceInfo(
//                    cameraService.getHost(),
//                    cameraService.getPort(),
//                    "", serviceName, bIsPcServer);
//                VdtCameraManager.getManager().connectCamera(serviceInfo);
//
//            }
//
//            @Override
//            public void onError(int errorCode) {
//
//            });

        CameraDiscovery.discoverCameras(this, new CameraDiscovery.Callback() {
            @Override
            public void onCameraFound(NsdServiceInfo cameraService) {
                String serviceName = cameraService.getServiceName();
                boolean bIsPcServer = serviceName.equals("Vidit Studio");
                final VdtCamera.ServiceInfo serviceInfo = new VdtCamera.ServiceInfo(
                    cameraService.getHost(),
                    cameraService.getPort(),
                    "", serviceName, bIsPcServer);
//                Logger.t("testconnect").d("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
                VdtCameraManager.getManager().connectCamera(serviceInfo);
            }

            @Override
            public void onError(int errorCode) {

            }
        });

        mScanner = new DeviceScanner(this);
        mScanner.startWork();

//        mNanoMdns = new NanoMdns(this) {
//            @Override
//            public void onServiceResoledAsync(NanoMdns mdns, VdtCamera.ServiceInfo serviceInfo) {
//                //Logger.t("test").d("onServiceResoledAsync");
//                VdtCameraManager.getManager().connectCamera(serviceInfo);
//                //mVdtCameraManager.connectCamera(serviceInfo);
//            }
//        };
//        mNanoMdns.startWork();
    }


    private void initCameraManager() {
        VdtCameraManager.initialize(this);

    }

    public static Context getContext() {
        return mSharedContext;
    }

    private void initFacebookSDK() {
        FacebookSdk.sdkInitialize(this);
    }

    private void initLogger() {
        Logger
            .init(TAG)
            .setMethodCount(1)
            .hideThreadInfo();
    }


}
