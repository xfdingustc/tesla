package com.waylens.hachi.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.orhanobut.logger.Logger;
import com.transee.ccam.CameraManager;
import com.transee.common.WifiAdmin;
import com.transee.vdb.DownloadAdmin;
import com.transee.vdb.DownloadService;
import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.views.PrefsUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class Hachi extends Application {
    private static final String TAG = Hachi.class.getSimpleName();
    public static final int WIFI_SCAN_INTERVAL = 3000;
    public static final String VIDEO_DOWNLOAD_PATH = "/Transee/video/Vidit/";
    public static final String PICTURE_DOWNLOAD_PATH = "/Transee/picture/Vidit/";

    static final boolean DEBUG = false;

    private Handler mHandler;
    private CameraManager mCameraManager;
    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        mHandler = new Handler();
        mCameraManager = new CameraManager(this);

        PrefsUtil.init(this);
        //initLogger();

        SessionManager.initialize(this);
        SessionManager.getInstance().reloadLoginInfo();
    }

    private void initLogger() {
        Logger
            .init(TAG)
            .setMethodCount(1)
            .hideThreadInfo();
    }




    static public class StorageInfo {
        public String firstStorage;
        public String secondStorage;
    }



    // API
    public CameraManager getCameraManager() {
        return mCameraManager;
    }

    static public void slideInFromLeft(Activity activity, boolean bPush) {
        if (!bPush) {
            activity.overridePendingTransition(R.anim.in_from_left, R.anim.keep);
        } else {
            activity.overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
        }
    }

    static public void slideOutToLeft(Activity activity, boolean bPush) {
        if (!bPush) {
            activity.overridePendingTransition(R.anim.keep, R.anim.out_to_left);
        } else {
            activity.overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
        }
    }

    static public void slideInFromRight(Activity activity, boolean bPush) {
        if (!bPush) {
            activity.overridePendingTransition(R.anim.in_from_right, R.anim.keep);
        } else {
            activity.overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
        }
    }

    static public void slideOutToRight(Activity activity, boolean bPush) {
        if (!bPush) {
            activity.overridePendingTransition(R.anim.keep, R.anim.out_to_right);
        } else {
            activity.overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
        }
    }

    static private final String getPath(String subdir) {
        //		String extStore = System.getenv("SECONDARY_STORAGE");
        //		Log.d(TAG, extStore);
        File sdCardDir = Environment.getExternalStorageDirectory();
        if (sdCardDir == null)
            return null;
        String dir = sdCardDir.toString() + subdir;
        File dirFile = new File(dir);
        dirFile.mkdirs();
        return dir;
    }

    public final void test() {
        // /data/data/com.transee.viditcam/files
        Context context = this.getApplicationContext();
        File tt = context.getFilesDir();
        Log.d(TAG, "files dir: " + tt.getPath());
        // /mnt/sdcard/Android/data/com.transee.viditcam/files
        tt = context.getExternalFilesDir(null);
        Log.d(TAG, "ext files dir: " + tt.getPath());
    }

    // API
    static public final String getVideoDownloadPath() {
        return getPath(VIDEO_DOWNLOAD_PATH);
    }

    // API
    static public final String getPicturePath() {
        return getPath(PICTURE_DOWNLOAD_PATH);
    }

    // API
    static public final void addToMediaStore(Context context, String filename) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(new File(filename)));
        context.sendBroadcast(intent);
    }

    // ------------------------------------------------------------------------------------

    // Wifi Admin
    private WifiAdmin mWifiAdmin;
    private Runnable mScanWifiAction;
    private ArrayList<WifiCallback> mWifiCallbackList = new ArrayList<WifiCallback>();

    // API
    public interface WifiCallback {
        public void networkStateChanged(WifiAdmin wifiAdmin);

        public void wifiScanResult(WifiAdmin wifiAdmin);

        public void onConnectError(WifiAdmin wifiAdmin);

        public void onConnectDone(WifiAdmin wifiAdmin);
    }

    // API
    public WifiAdmin attachWifiAdmin(WifiCallback callback) {
        if (DEBUG) {
            Log.d(TAG, "attachWifiAdmin " + callback);
        }
        mWifiCallbackList.add(callback);
        if (mWifiAdmin == null) {
            if (DEBUG) {
                Log.d(TAG, "start WifiAdmin");
            }
            mWifiAdmin = new MyWifiAdmin(this);
            mWifiAdmin.init();
            mScanWifiAction = new Runnable() {
                @Override
                public void run() {
                    requestScan();
                }
            };
            requestScan();
        }
        return mWifiAdmin;
    }

    private void requestScan() {
        if (mWifiAdmin != null && mScanWifiAction != null) {
            mWifiAdmin.scan();
            if (mScanWifiAction != null) {
                mHandler.postDelayed(mScanWifiAction, WIFI_SCAN_INTERVAL);
            }
        }
    }

    // API
    public void detachWifiAdmin(WifiCallback callback, boolean bCancelConnect) {
        mWifiCallbackList.remove(callback);
        if (bCancelConnect) {
            mWifiAdmin.cancelConnect();
        }
        if (mWifiCallbackList.size() == 0) {
            if (DEBUG) {
                Log.d(TAG, "stop WifiAdmin");
            }
            if (mScanWifiAction != null) {
                mHandler.removeCallbacks(mScanWifiAction);
                mScanWifiAction = null;
            }
            mWifiAdmin.release();
            mWifiAdmin = null;
        }
        if (DEBUG) {
            Log.d(TAG, "detachWifiAdmin " + callback);
        }
    }

    // API
    public WifiAdmin getWifiAdmin() {
        return mWifiAdmin;
    }

    private void networkStateChanged(WifiAdmin wifiAdmin) {
        if (DEBUG) {
            Log.d(TAG, "networkStateChanged");
        }
        for (WifiCallback callback : mWifiCallbackList) {
            callback.networkStateChanged(wifiAdmin);
        }
    }

    private void wifiScanResult(WifiAdmin wifiAdmin) {
        if (DEBUG) {
            Log.d(TAG, "wifiScanResult");
        }
        for (WifiCallback callback : mWifiCallbackList) {
            callback.wifiScanResult(wifiAdmin);
        }
    }

    private void onConnectError(WifiAdmin wifiAdmin) {
        if (DEBUG) {
            Log.d(TAG, "onConnectError");
        }
        for (WifiCallback callback : mWifiCallbackList) {
            callback.onConnectError(wifiAdmin);
        }
    }

    private void onConnectDone(WifiAdmin wifiAdmin) {
        if (DEBUG) {
            Log.d(TAG, "onConnectDone");
        }
        for (WifiCallback callback : mWifiCallbackList) {
            callback.onConnectDone(wifiAdmin);
        }
    }

    class MyWifiAdmin extends WifiAdmin {

        public MyWifiAdmin(Context context) {
            super(context);
        }

        @Override
        public void networkStateChanged(WifiAdmin wifiAdmin) {
            if (wifiAdmin == mWifiAdmin) {
                Hachi.this.networkStateChanged(wifiAdmin);
            }
        }

        @Override
        public void wifiScanResult(WifiAdmin wifiAdmin) {
            if (wifiAdmin == mWifiAdmin) {
                Hachi.this.wifiScanResult(wifiAdmin);
            }
        }

        @Override
        public void onConnectError(WifiAdmin wifiAdmin) {
            if (wifiAdmin == mWifiAdmin) {
                Hachi.this.onConnectError(wifiAdmin);
            }
        }

        @Override
        public void onConnectDone(WifiAdmin wifiAdmin) {
            if (wifiAdmin == mWifiAdmin) {
                Hachi.this.onConnectDone(wifiAdmin);
            }
        }

    }

    // ------------------------------------------------------------------------------------

    private DownloadAdmin mDownloadAdmin;
    private ArrayList<DownloadCallback> mDownloadCallbackList = new ArrayList<DownloadCallback>();

    public interface DownloadCallback {
        public void onDownloadInfo(DownloadService.DownloadInfo downloadInfo);
    }

    private void onDownloadInfoChanged() {
        DownloadService.DownloadInfo downloadInfo = mDownloadAdmin.getDownloadInfo();
        for (int i = 0; i < mDownloadCallbackList.size(); i++) {
            DownloadCallback callback = mDownloadCallbackList.get(i);
            callback.onDownloadInfo(downloadInfo);
        }
    }

    public DownloadAdmin attachDownloadAdmin(DownloadCallback callback) {
        mDownloadCallbackList.add(callback);
        if (mDownloadAdmin == null) {
            mDownloadAdmin = new DownloadAdmin(this) {
                @Override
                public void downloadInfoChanged(DownloadAdmin admin) {
                    if (admin == mDownloadAdmin) {
                        onDownloadInfoChanged();
                    }
                }
            };
        }
        return mDownloadAdmin;
    }

    public void detachDownloadAdmin(DownloadCallback callback) {
        mDownloadCallbackList.remove(callback);
        if (mDownloadCallbackList.size() == 0) {
            if (mDownloadAdmin != null) {
                mDownloadAdmin.release();
                mDownloadAdmin = null;
            }
        }
    }
}
