package com.waylens.hachi.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.multidex.MultiDexApplication;

import com.facebook.FacebookSdk;
import com.github.piasy.cameracompat.CameraCompat;
import com.google.android.exoplayer.ExoPlayerLibraryInfo;
import com.orhanobut.logger.Logger;
import com.squareup.leakcanary.LeakCanary;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.export.ExportManager;
import com.waylens.hachi.camera.connectivity.VdtCameraConnectivityManager;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.uploadqueue.UploadManager;
import com.waylens.hachi.uploadqueue.model.DaoMaster;
import com.waylens.hachi.uploadqueue.model.DaoSession;
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

    private static String mUserAgent;

    private static DaoMaster mDaoMaster;
    private static DaoSession mDaoSession;

    public static DaoMaster getDaoMaster() {
        if (mDaoMaster == null) {
            DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(mSharedContext, "upload14", null);
            mDaoMaster = new DaoMaster(helper.getWritableDatabase());
        }

        return mDaoMaster;
    }

    public static DaoSession getDaoSession() {
        if (mDaoSession == null) {
            mDaoSession = getDaoMaster().newSession();
        }

        return mDaoSession;
    }


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
        VdtCameraConnectivityManager.getManager().stopSearchCamera();
    }


    private void init() {
        mSharedContext = getApplicationContext();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
//        LeakCanary.install(this);
        initLogger();

        configureJobManager();

        ExportManager.getManager();

        PreferenceUtils.initialize(this);

        UploadManager uploadManager = UploadManager.getManager(this);

        initSessionInfo();

        initFacebookSDK();

//        FIR.init(this);

//        ConnectivityHelper.setPreferredNetwork(ConnectivityManager.TYPE_WIFI);

        VdtCameraConnectivityManager.getManager().startSearchCamera();
        mUserAgent = initUserAgent(getString(R.string.app_name));

        CameraCompat.init(getApplicationContext());
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

    private String initUserAgent(String applicationName) {
        String versionName;
        try {

            String packageName = getPackageName();
            PackageInfo info = getPackageManager().getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "?";
        }
        return applicationName + "/" + versionName + " (Linux;Android " + Build.VERSION.RELEASE
            + ") " + "ExoPlayerLib/" + ExoPlayerLibraryInfo.VERSION;
    }

    public static String getUserAgent() {
        return mUserAgent;
    }


}
