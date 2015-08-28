package com.waylens.hachi.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.ui.services.DownloadService;
import com.waylens.hachi.hardware.VdtCameraManager;
import com.waylens.hachi.hardware.WifiAdminManager;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.views.PrefsUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class Hachi extends Application {
    private static final String TAG = Hachi.class.getSimpleName();

    public static final String VIDEO_DOWNLOAD_PATH = "/Transee/video/Vidit/";
    public static final String PICTURE_DOWNLOAD_PATH = "/Transee/picture/Vidit/";

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {

        PrefsUtil.init(this);
        initLogger();

        PreferenceUtils.initialize(this);

        SessionManager.initialize(this);
        SessionManager.getInstance().reloadLoginInfo();

        initCameraManager();

        WifiAdminManager.initialize(this);
        initFacebookSDK();
        ImageUtils.initImageLoader(this);
    }

    private void initCameraManager() {
        VdtCameraManager.initialize(this);

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



    private ArrayList<DownloadCallback> mDownloadCallbackList = new ArrayList<DownloadCallback>();

    public interface DownloadCallback {
        public void onDownloadInfo(DownloadService.DownloadInfo downloadInfo);
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
