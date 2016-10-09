package com.waylens.hachi.app;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.support.multidex.MultiDexApplication;

import com.facebook.FacebookSdk;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.upload.UploadManager;
import com.waylens.hachi.camera.CameraDiscovery;
import com.waylens.hachi.camera.DeviceScanner;
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.utils.PreferenceUtils;


//import com.bugtags.library.Bugtags;
//import com.bugtags.library.BugtagsOptions;
//import com.tencent.bugly.crashreport.CrashReport;


/**
 * Created by Xiaofei on 2015/8/4.
 */
public class Hachi extends MultiDexApplication {
    private static final String TAG = Hachi.class.getSimpleName();


    private static Context mSharedContext = null;


    private DeviceScanner mScanner;


    @Override
    public void onCreate() {
        super.onCreate();
        /**
         * inti Bugtags
         */
//        BugtagsOptions options = new BugtagsOptions.Builder().
//            trackingLocation(true).
//            trackingCrashLog(true).
//            trackingConsoleLog(true).
//            trackingUserSteps(true).
//            versionName("0.38").
//            versionCode(1).
//            build();
//        Bugtags.start("a088e06d7c05be80a41cf34e7de0f9b0", this, Bugtags.BTGInvocationEventNone, options);
//
//        /**
//         * inti Burly
//         */
//        CrashReport.initCrashReport(getApplicationContext(), "900022478", false);

        init();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        CameraDiscovery.stopDiscovery();
        mScanner.stopWork();
    }


    private void init() {
        mSharedContext = getApplicationContext();

//        LeakCanary.install(this);

        initLogger();

        UploadManager uploadManager = UploadManager.getManager();

        configureJobManager();

        PreferenceUtils.initialize(this);

        initSessionInfo();


        initFacebookSDK();


//        FIR.init(this);


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

    private void initSessionInfo() {
        SessionManager.initialize(this);
        SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.reloadLoginInfo();
        if (sessionManager.isLoggedIn() && !sessionManager.isVerified()) {
            sessionManager.reloadVerifyInfo();
        }

    }

    private void configureJobManager() {
        BgJobManager.init(this);
    }

    public void startDeviceScanner() {
        if (mScanner != null) {
            mScanner.stopWork();
        }
        mScanner = new DeviceScanner(this);
        mScanner.startWork();
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
            .hideThreadInfo()
            .methodCount(1);

    }


}
