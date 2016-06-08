package com.waylens.hachi.ui.settings;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.BuildConfig;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.utils.PreferenceUtils;

import org.json.JSONObject;

import java.io.File;


import butterknife.BindView;
import butterknife.OnClick;
import im.fir.sdk.FIR;
import im.fir.sdk.VersionCheckCallback;


/**
 * Created by Richard on 1/8/16.
 */
public class VersionCheckActivity extends BaseActivity {

    private static final String TAG = "FIR";
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0x10;

    private int mVersionCode;
    private String mVersionName;
    private String mInstallURL;
    String mDownloadedFile;
    DownloadManager downloadManager;

    private Snackbar mChangeWebServerSnack;
    private int mClickCount = 5;

    @BindView(R.id.current_version_view)
    TextView mCurrentVersionView;

    @BindView(R.id.fir_version_view)
    TextView mFirVersionView;

    @BindView(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @BindView(R.id.btn_update_now)
    View mBtnUpdateNow;

    @OnClick(R.id.waylens_logo)
    public void onWaylensLogoClicked() {
        if (mChangeWebServerSnack == null || !mChangeWebServerSnack.isShown()) {
            mClickCount = 5;
            String snakeBar = "" + mClickCount + " clicks to change web server";
            mChangeWebServerSnack = Snackbar.make(mFirVersionView, snakeBar, Snackbar.LENGTH_SHORT);
            mChangeWebServerSnack.show();
        } else {
            mClickCount--;
            if (mClickCount == 0) {
                MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .items(R.array.server_list)
                    .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                            return false;
                        }
                    })
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Logger.t(TAG).d("select index: " + getResources().getStringArray(R.array.server_list)[dialog.getSelectedIndex()]);
                            PreferenceUtils.putString("server", getResources().getStringArray(R.array.server_list)[dialog.getSelectedIndex()]);
                        }
                    })
                    .show();
            } else {
                String snakeBar = "" + mClickCount + " clicks to change web server";
                mChangeWebServerSnack = Snackbar.make(mFirVersionView, snakeBar, Snackbar.LENGTH_SHORT);
                mChangeWebServerSnack.show();
            }
        }

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



    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, VersionCheckActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        FIR.init(getApplicationContext());
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_vesion);
        mCurrentVersionView.setText(getString(R.string.current_version) + BuildConfig.VERSION_NAME);
        mViewAnimator.setDisplayedChild(2);
    }


    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @OnClick(R.id.btn_check_update)
    void checkUpdate() {
        mViewAnimator.setDisplayedChild(0);

        FIR.checkForUpdateInFIR("de9cc37998f3f6ad143a8b608cc7968f", new VersionCheckCallback() {
            @Override
            public void onSuccess(String response) {
                super.onSuccess(response);
                Logger.t(TAG).d("Msg: " + response);
                if (response == null) {
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    mVersionCode = Integer.parseInt(jsonObject.getString("build"));
                    mVersionName = jsonObject.getString("versionShort");
                    mInstallURL = jsonObject.getString("install_url");

                    if (mVersionCode <= BuildConfig.VERSION_CODE) {
                        mFirVersionView.setText(R.string.version_up_to_date);
                        mViewAnimator.setDisplayedChild(1);
                    } else {
                        mFirVersionView.setText(getString(R.string.fir_version) + mVersionName);
                        mViewAnimator.setDisplayedChild(3);
                    }
                } catch (Exception e) {
                    Logger.t(TAG).d("" + e);
                    mFirVersionView.setText(R.string.error_check_fir);
                    mViewAnimator.setDisplayedChild(2);
                }
            }

            @Override
            public void onFail(Exception e) {
                super.onFail(e);
                Logger.t(TAG).d("" + e);
                mFirVersionView.setText(R.string.error_check_fir);
                mViewAnimator.setDisplayedChild(2);
            }

            @Override
            public void onStart() {
                super.onStart();
                Logger.t(TAG).d("onStart");
            }

            @Override
            public void onFinish() {
                super.onFinish();
                mFirVersionView.setVisibility(View.VISIBLE);
            }
        });
    }


    void downloadUpdateAPK() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            continueDownloadAPK();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }


    }

    public void continueDownloadAPK() {
        if (mInstallURL == null) {
            return;
        }
        mFirVersionView.setText(R.string.downloading);
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mInstallURL));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        request.setAllowedOverRoaming(true);
        request.setMimeType("application/vnd.android.package-archive");
        request.setVisibleInDownloadsUi(true);
        String apkFile = "waylens-v" + mVersionCode + ".apk";
        request.setDestinationInExternalPublicDir("/download/", apkFile);
        request.setTitle(getString(R.string.app_name) + " v" + mVersionName);
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                int i = 0;
                for (String permission : permissions) {
                    if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            continueDownloadAPK();
                        } else {
                            mFirVersionView.setText(R.string.error_no_write_external_storage_permission);
                            mViewAnimator.setDisplayedChild(3);
                        }
                        break;
                    }
                    i++;
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            queryDownloadStatus();
        }
    };
}