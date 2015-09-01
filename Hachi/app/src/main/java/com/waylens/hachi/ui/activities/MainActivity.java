package com.waylens.hachi.ui.activities;


import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.TabLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.waylens.hachi.BuildConfig;
import com.waylens.hachi.R;
import com.waylens.hachi.gcm.RegistrationIntentService;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.fragments.AccountFragment;
import com.waylens.hachi.ui.fragments.CommentsFragment;
import com.waylens.hachi.ui.fragments.HomeFragment;
import com.waylens.hachi.ui.fragments.LiveFragment;
import com.waylens.hachi.ui.fragments.NotificationFragment;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.PushUtils;

import java.io.File;

import butterknife.Bind;
import im.fir.sdk.FIR;
import im.fir.sdk.callback.VersionCheckCallback;
import im.fir.sdk.version.AppVersion;

public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String TAB_HOME_TAG = "home";
    private static final String TAB_LIVE_TAG = "live";
    private static final String TAB_HIGHLIGHTS_TAG = "highlights";
    private static final String TAB_NOTIFICATIONS_TAG = "notification";
    private static final String TAB_ACCOUNT_TAG = "account";

    @Bind(R.id.main_tabs)
    TabLayout mMainTabs;

    private DownloadManager downloadManager;

    String apkFile;

    private class BottomTab {
        private int mIconRes;
        private String mTabTag;

        public BottomTab(int iconRes, String tabTag) {
            this.mIconRes = iconRes;
            this.mTabTag = tabTag;
        }
    }

    private BottomTab mTabList[] = {
            new BottomTab(R.drawable.ic_home, TAB_HOME_TAG),
            new BottomTab(R.drawable.ic_live, TAB_LIVE_TAG),
            new BottomTab(R.drawable.ic_highlights, TAB_HIGHLIGHTS_TAG),
            new BottomTab(R.drawable.ic_notifications, TAB_NOTIFICATIONS_TAG),
            new BottomTab(R.drawable.ic_account, TAB_ACCOUNT_TAG)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void init() {
        super.init();
        initViews();
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        FIR.checkForUpdateInFIR("de9cc37998f3f6ad143a8b608cc7968f", new VersionCheckCallback() {
            @Override
            public void onSuccess(AppVersion appVersion, boolean result) {
                Log.e("FIR", "onSuccess: thisCode: " + BuildConfig.VERSION_CODE + "; result: " + result);
                if (appVersion.getVersionCode() > BuildConfig.VERSION_CODE) {
                    downloadUpdateAPK(appVersion.getUpdateUrl(), appVersion.getVersionName());
                }
            }

            @Override
            public void onFail(String s, int i) {
                Log.e("FIR", "onFail: " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.e("FIR", "onError");
            }

            @Override
            public void onStart() {
                Log.e("FIR", "onStart");
            }

            @Override
            public void onFinish() {
                Log.e("FIR", "onFinish");
            }
        });

        if (SessionManager.getInstance().isLoggedIn() && PushUtils.checkGooglePlayServices(this)) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    private void initViews() {
        setContentView(R.layout.activity_main);
        for (int i = 0; i < mTabList.length; i++) {
            TabLayout.Tab tab = mMainTabs.newTab();
            tab.setIcon(mTabList[i].mIconRes);
            tab.getIcon().setColorFilter(getResources().getColor(R.color.material_grey_500),
                    PorterDuff.Mode.MULTIPLY);
            mMainTabs.addTab(tab);
        }

        mMainTabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tab.getIcon().setColorFilter(getResources().getColor(R.color.style_color_primary),
                        PorterDuff.Mode.MULTIPLY);
                if (tab.getPosition() == 2) {
                    startActivity(new Intent(MainActivity.this, CameraListActivity.class));
                } else {
                    String tag = mTabList[tab.getPosition()].mTabTag;
                    switchFragment(tag);
                }

                tab.getIcon().setColorFilter(getResources().getColor(R.color.style_color_primary),
                        PorterDuff.Mode.MULTIPLY);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.getIcon().setColorFilter(getResources().getColor(R.color.material_grey_500),
                        PorterDuff.Mode.MULTIPLY);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        setDefaultFirstFragment(TAB_HOME_TAG);
        TabLayout.Tab tab = mMainTabs.getTabAt(0);
        tab.getIcon().setColorFilter(getResources().getColor(R.color.style_color_primary),
                PorterDuff.Mode.MULTIPLY);

        if (!SessionManager.getInstance().isLoggedIn()) {
            mMainTabs.getTabAt(4).select();
        }
    }

    private void setDefaultFirstFragment(String tag) {
        switchFragment(tag);
    }

    private void switchFragment(String tag) {
        Fragment fragment;
        if (tag.equals(TAB_HOME_TAG)) {
            fragment = new HomeFragment();
        } else if (tag.equals(TAB_LIVE_TAG)) {
            fragment = new LiveFragment();
        } else if (tag.equals(TAB_NOTIFICATIONS_TAG)) {
            fragment = new NotificationFragment();
        } else {
            fragment = new AccountFragment();
        }
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_content, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        //mToolbar.setTitle(R.string.);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.root_container);
        if (fragment != null) {
            Bundle args = fragment.getArguments();
            final int position = args.getInt(CommentsFragment.ARG_MOMENT_POSITION);
            final long momentID = args.getLong(CommentsFragment.ARG_MOMENT_ID);
            boolean hasUpdates = args.getBoolean(CommentsFragment.ARG_HAS_UPDATES);

            Fragment homeFragment = getFragmentManager().findFragmentById(R.id.fragment_content);
            if (hasUpdates && homeFragment != null && homeFragment instanceof HomeFragment) {
                HomeFragment fg = (HomeFragment) homeFragment;
                fg.loadComment(momentID, position);
            }
            getFragmentManager().beginTransaction().remove(fragment).commit();

        } else {
            super.onBackPressed();
        }
    }

    void downloadUpdateAPK(String url, String versionName) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        request.setAllowedOverRoaming(false);
        request.setMimeType("application/vnd.android.package-archive");
        request.setVisibleInDownloadsUi(true);
        apkFile = "waylens-v" + versionName + ".apk";
        request.setDestinationInExternalPublicDir("/download/", apkFile);
        request.setTitle(getString(R.string.app_name) + " v" + versionName);
        long id = downloadManager.enqueue(request);

        PreferenceUtils.putLong("download_id", id);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            queryDownloadStatus();
        }
    };

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
                    File file = new File(Environment.getExternalStoragePublicDirectory("download"), apkFile);
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
}
