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
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.waylens.hachi.BuildConfig;
import com.waylens.hachi.R;
import com.waylens.hachi.gcm.RegistrationIntentService;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.fragments.AccountFragment;
import com.waylens.hachi.ui.fragments.CameraListFragment;
import com.waylens.hachi.ui.fragments.CommentsFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.HomeFragment;
import com.waylens.hachi.ui.fragments.LiveFragment;
import com.waylens.hachi.ui.fragments.NotificationFragment;
import com.waylens.hachi.ui.fragments.Refreshable;
import com.waylens.hachi.ui.fragments.YouTubeFragment;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.PushUtils;

import java.io.File;

import butterknife.Bind;
import im.fir.sdk.FIR;
import im.fir.sdk.callback.VersionCheckCallback;
import im.fir.sdk.version.AppVersion;

public class MainActivity extends BaseActivity implements AppBarLayout.OnOffsetChangedListener, TabSwitchable {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int TAB_HOME_TAG = 0;
    private static final int TAB_LIVE_TAG = 1;
    private static final int TAB_HIGHLIGHTS_TAG = 2;
    private static final int TAB_NOTIFICATIONS_TAG = 3;
    private static final int TAB_ACCOUNT_TAG = 4;

    @Bind(R.id.main_tabs)
    TabLayout mMainTabs;

    @Bind(R.id.app_bar_layout)
    AppBarLayout mAppBarLayout;

    private DownloadManager downloadManager;

    String apkFile;

    private BottomTab mTabList[] = {
            new BottomTab(R.drawable.ic_home, TAB_HOME_TAG),
            new BottomTab(R.drawable.ic_live, TAB_LIVE_TAG),
            new BottomTab(R.drawable.ic_highlights, TAB_HIGHLIGHTS_TAG),
            new BottomTab(R.drawable.ic_notifications, TAB_NOTIFICATIONS_TAG),
            new BottomTab(R.drawable.ic_account, TAB_ACCOUNT_TAG)
    };

    private Bundle fragmentArgs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        mAppBarLayout.addOnOffsetChangedListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAppBarLayout.removeOnOffsetChangedListener(this);
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

    @SuppressWarnings("ConstantConditions")
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
                switchFragment(mTabList[tab.getPosition()].mTabTag);
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

        switchFragment(TAB_HOME_TAG);
        TabLayout.Tab tab = mMainTabs.getTabAt(0);
        tab.getIcon().setColorFilter(getResources().getColor(R.color.style_color_primary),
                PorterDuff.Mode.MULTIPLY);

        if (!SessionManager.getInstance().isLoggedIn()) {
            mMainTabs.getTabAt(4).select();
        }
    }

    void enableRefresh(boolean enabled) {
        Fragment currentFragment = getFragmentManager().findFragmentById(R.id.fragment_content);
        if (currentFragment instanceof Refreshable) {
            ((Refreshable) currentFragment).enableRefresh(enabled);
        }
    }

    private void switchFragment(int tag) {

        Fragment fragment;
        switch (tag) {
            case TAB_HOME_TAG:
                fragment = new HomeFragment();
                break;
            case TAB_LIVE_TAG:
                fragment = new LiveFragment();
                if (fragmentArgs != null) {
                    fragment.setArguments(fragmentArgs);
                }
                break;
            case TAB_HIGHLIGHTS_TAG:
                fragment = new CameraListFragment();
                break;
            case TAB_NOTIFICATIONS_TAG:
                fragment = new NotificationFragment();
                break;
            case TAB_ACCOUNT_TAG:
                fragment = new AccountFragment();
                break;
            default:
                fragment = new HomeFragment();
        }

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_content, fragment);
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
            DashboardActivity.launch(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (YouTubeFragment.fullScreenFragment != null) {
            ((FragmentNavigator)YouTubeFragment.fullScreenFragment).onBack();
            return;
        }

        Fragment fragment = getFragmentManager().findFragmentById(R.id.root_container);
        if (fragment instanceof FragmentNavigator) {
            ((FragmentNavigator) fragment).onBack();
            return;
        }

        super.onBackPressed();
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

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        enableRefresh(i == 0);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void switchTab(int position, Bundle args) {
        if (position < 0 || position > mMainTabs.getTabCount()) {
            return;
        }
        fragmentArgs = args;
        mMainTabs.getTabAt(position).select();
    }

    static class BottomTab {
        private int mIconRes;
        private int mTabTag;

        public BottomTab(int iconRes, int tabTag) {
            this.mIconRes = iconRes;
            this.mTabTag = tabTag;
        }
    }
}
