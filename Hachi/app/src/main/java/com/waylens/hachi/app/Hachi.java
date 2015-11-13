package com.waylens.hachi.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import android.util.Log;

import com.facebook.FacebookSdk;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.hardware.VdtCameraManager;
import com.waylens.hachi.hardware.WifiAdminManager;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.skin.SkinManager;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.ui.services.download.DownloadService;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.utils.PreferenceUtils;

import java.io.File;
import java.util.ArrayList;

import im.fir.sdk.FIR;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class Hachi extends Application {
    private static final String TAG = Hachi.class.getSimpleName();



    private static Context mSharedContext = null;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
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

        WifiAdminManager.initialize(this);
        initFacebookSDK();
        ImageUtils.initImageLoader(this);

        FIR.init(this);

        Snipe.init();

        SkinManager.initialize(this);
        SkinManager.getManager().load();
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
    static public final void addToMediaStore(Context context, String filename) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(new File(filename)));
        context.sendBroadcast(intent);
    }



    private ArrayList<DownloadCallback> mDownloadCallbackList = new ArrayList<DownloadCallback>();

    public interface DownloadCallback {
        void onDownloadInfo(DownloadService.DownloadStatusInfo downloadInfo);
    }

    /*
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
    }*/
}
