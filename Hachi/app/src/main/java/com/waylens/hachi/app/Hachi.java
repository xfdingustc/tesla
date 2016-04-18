package com.waylens.hachi.app;

import android.app.Application;
import android.content.Context;
import android.net.nsd.NsdServiceInfo;

import com.bugtags.library.Bugtags;
import com.bugtags.library.BugtagsOptions;
import com.facebook.FacebookSdk;
import com.orhanobut.logger.Logger;
import com.squareup.leakcanary.LeakCanary;
import com.tencent.bugly.crashreport.CrashReport;
import com.waylens.hachi.dao.RawDataItemDao;
import com.waylens.hachi.hardware.CameraDiscovery;
import com.waylens.hachi.hardware.DeviceScanner;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.utils.PreferenceUtils;


/**
 * Created by Xiaofei on 2015/8/4.
 */
public class Hachi extends Application {
    private static final String TAG = Hachi.class.getSimpleName();


    private static Context mSharedContext = null;

    private DeviceScanner mScanner;


    @Override
    public void onCreate() {
        super.onCreate();
        /**
         * inti Bugtags
         */
        BugtagsOptions options = new BugtagsOptions.Builder().
            trackingLocation(true).
            trackingCrashLog(true).
            trackingConsoleLog(true).
            trackingUserSteps(true).
            versionName("0.38").
            versionCode(1).
            build();
        Bugtags.start("a088e06d7c05be80a41cf34e7de0f9b0", this, Bugtags.BTGInvocationEventNone, options);

        /**
         * inti Burly
         */
        CrashReport.initCrashReport(getApplicationContext(), "900022478", false);

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

        LeakCanary.install(this);

        initLogger();

        PreferenceUtils.initialize(this);

        SessionManager.initialize(this);
        SessionManager.getInstance().reloadLoginInfo();

        RawDataItemDao.initialize(this);

        initCameraManager();

        initFacebookSDK();
        ImageUtils.initImageLoader(this);
        VdbImageLoader.getImageLoader(null).init(this, 1024 * 64);
//        FIR.init(this);

        Snipe.init();

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
                Logger.t(TAG).e("errorCode: " + errorCode);
            }
        });

        startDeviceScanner();
    }

    public void startDeviceScanner() {
        if (mScanner != null) {
            mScanner.stopWork();
        }
        mScanner = new DeviceScanner(this);
        mScanner.startWork();
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
