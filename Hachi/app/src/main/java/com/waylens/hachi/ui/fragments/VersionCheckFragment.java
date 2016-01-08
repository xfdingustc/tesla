package com.waylens.hachi.ui.fragments;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.squareup.okhttp.Request;
import com.waylens.hachi.BuildConfig;
import com.waylens.hachi.R;
import com.waylens.hachi.utils.PreferenceUtils;

import java.io.File;

import butterknife.Bind;
import butterknife.OnClick;
import im.fir.sdk.FIR;
import im.fir.sdk.callback.VersionCheckCallback;
import im.fir.sdk.version.AppVersion;

/**
 * Created by Richard on 1/8/16.
 */
public class VersionCheckFragment extends BaseFragment implements FragmentNavigator {

    private static final String TAG = "FIR";

    @Bind(R.id.current_version_view)
    TextView mCurrentVersionView;

    @Bind(R.id.fir_version_view)
    TextView mFirVersionView;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.btn_update_now)
    View mBtnUpdateNow;

    AppVersion mAppVersion;
    String mDownloadedFile;
    DownloadManager downloadManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_vesion, savedInstanceState);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCurrentVersionView.setText(getString(R.string.current_version, BuildConfig.VERSION_NAME));
        mViewAnimator.setDisplayedChild(2);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(receiver);
    }

    @OnClick(R.id.btn_check_update)
    void checkUpdate() {
        mViewAnimator.setDisplayedChild(0);
        FIR.checkForUpdateInFIR("de9cc37998f3f6ad143a8b608cc7968f", new VersionCheckCallback() {
            @Override
            public void onSuccess(AppVersion appVersion, boolean result) {
                Log.e("FIR", "onSuccess: thisCode: " + BuildConfig.VERSION_CODE + "; result: " + result);
                Log.e("FIR", "onSuccess: AppVersion: " + appVersion);

                if (appVersion == null) {
                    return;
                }
                int versionCode = appVersion.getVersionCode();
                if (versionCode <= BuildConfig.VERSION_CODE) {
                    mFirVersionView.setText(R.string.version_up_to_date);
                    mViewAnimator.setDisplayedChild(1);
                } else {
                    mAppVersion = appVersion;
                    mFirVersionView.setText(getString(R.string.fir_version, versionCode));
                    mViewAnimator.setDisplayedChild(3);
                }

            }

            @Override
            public void onFail(Request request, Exception e) {
                Log.e("FIR", "", e);
                mFirVersionView.setText(R.string.error_check_fir);
                mViewAnimator.setDisplayedChild(2);
            }

            @Override
            public void onStart() {
                Log.e("FIR", "onStart");
            }

            @Override
            public void onFinish() {
                mFirVersionView.setVisibility(View.VISIBLE);
            }
        });
    }

    @OnClick(R.id.btn_update_now)
    void updateApp() {
        mViewAnimator.setDisplayedChild(0);
        downloadUpdateAPK();
    }

    @OnClick(R.id.btn_install_now)
    void installAPK() {
        if (mDownloadedFile == null) {
            return;
        }

        File file = new File(mDownloadedFile);
        if (file.exists()) {
            installAPK(file);
        }
    }

    void downloadUpdateAPK() {
        if (mAppVersion == null) {
            return;
        }
        downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mAppVersion.getUpdateUrl()));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        request.setAllowedOverRoaming(true);
        request.setMimeType("application/vnd.android.package-archive");
        request.setVisibleInDownloadsUi(true);
        String apkFile = "waylens-v" + mAppVersion.getVersionCode() + ".apk";
        request.setDestinationInExternalPublicDir("/download/", apkFile);
        request.setTitle(getString(R.string.app_name) + " v" + mAppVersion.getVersionName());
        long id = downloadManager.enqueue(request);
        PreferenceUtils.putLong("download_id", id);
    }

    void queryDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        long downloadId = PreferenceUtils.getLong("download_id", 0);
        query.setFilterById(downloadId);
        Cursor c = downloadManager.query(query);
        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                case DownloadManager.STATUS_PAUSED:
                    Log.v(TAG, "STATUS_PAUSED");
                case DownloadManager.STATUS_PENDING:
                    Log.v(TAG, "STATUS_PENDING");
                case DownloadManager.STATUS_RUNNING:
                    Log.v(TAG, "STATUS_RUNNING");
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    Log.v(TAG, "File is downloaded.");
                    mFirVersionView.setText("APK is download!");
                    mViewAnimator.setDisplayedChild(4);
                    mDownloadedFile = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                    Log.e(TAG, "Filename: " + mDownloadedFile);
                    File file = new File(mDownloadedFile);
                    if (file.exists()) {
                        installAPK(file);
                    }
                    PreferenceUtils.remove("download_id");
                    break;
                case DownloadManager.STATUS_FAILED:
                    Log.v(TAG, "STATUS_FAILED");
                    downloadManager.remove(downloadId);
                    PreferenceUtils.remove("download_id");
                    break;
            }
        }
    }

    void installAPK(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public boolean onInterceptBackPressed() {
        getFragmentManager().beginTransaction().replace(R.id.fragment_content, new SettingsFragment()).commit();
        return true;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            queryDownloadStatus();
        }
    };
}
