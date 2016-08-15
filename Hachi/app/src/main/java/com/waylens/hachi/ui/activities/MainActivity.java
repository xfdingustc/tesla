package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.lapism.searchview.SearchView;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.gcm.RegistrationIntentService;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.clips.ClipVideoFragment;
import com.waylens.hachi.ui.community.CommunityFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.liveview.CameraPreviewFragment;
import com.waylens.hachi.ui.settings.AccountActivity;
import com.waylens.hachi.ui.settings.SettingsFragment;
import com.waylens.hachi.utils.PreferenceUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2016/1/15.
 */
public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
//    private ActionBarDrawerToggle mDrawerToggle;

    public static final int TAB_TAG_VIDEO = 0;
    public static final int TAB_TAG_LIVE_VIEW = 1;
    public static final int TAB_TAG_MOMENTS = 2;
    public static final int TAB_TAG_SETTINGS = 3;

    public static final int REQUEST_CODE_SIGN_UP_FROM_MOMENTS = 100;

    private int mCurrentNavMenuId;

    //private BiMap<Integer, Integer> mMenuId2Tab = HashBiMap.create();
    private Map<Integer, Integer> mMenuId2Tab = new HashMap<>();
    private Map<Integer, Integer> mTab2MenuId = new HashMap<>();

    private Fragment[] mFragmentList = new Fragment[]{
        new ClipVideoFragment(),
        new CameraPreviewFragment(),
        new CommunityFragment(),
        new SettingsFragment()
    };

    private Fragment mCurrentFragment = null;
    private SessionManager mSessionManager = SessionManager.getInstance();

    @BindView(R.id.drawerLayout)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.navView)
    NavigationView mNavView;


    private CircleImageView mUserAvatar;
    private TextView mUsername;
    private boolean mIsRestored;

    private Snackbar mReturnSnackBar;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsRestored = true;
        } else {
            mIsRestored = false;
        }
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        refressNavHeaderView();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            hideSystemUI(true);
        } else {
            hideSystemUI(false);
        }
    }

    @Override
    protected void init() {
        super.init();
        mMenuId2Tab.put(R.id.moments, TAB_TAG_MOMENTS);
        mMenuId2Tab.put(R.id.setting, TAB_TAG_SETTINGS);
        mMenuId2Tab.put(R.id.video, TAB_TAG_VIDEO);
        mMenuId2Tab.put(R.id.liveView, TAB_TAG_LIVE_VIEW);

        mTab2MenuId.put(TAB_TAG_MOMENTS, R.id.moments);
        mTab2MenuId.put(TAB_TAG_SETTINGS, R.id.setting);
        mTab2MenuId.put(TAB_TAG_VIDEO, R.id.video);
        mTab2MenuId.put(TAB_TAG_LIVE_VIEW, R.id.liveView);

        initViews();

        RegistrationIntentService.launch(this);

        if (VdtCameraManager.getManager().isConnected()) {
            initFragment(TAB_TAG_LIVE_VIEW);
        } else {
            initFragment(TAB_TAG_MOMENTS);
        }
    }


    private void initViews() {
        setContentView(R.layout.activity_main);
        setupNavigationView();


    }

    private void hideSystemUI(boolean hide) {
        if (hide) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(lp);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            WindowManager.LayoutParams attr = getWindow().getAttributes();
            attr.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(attr);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }


    public void initFragment(int tag) {
        int menuId = mTab2MenuId.get(tag);

        // When init current menu id is 0. so here must check if MenuItem is null
        MenuItem item = mNavView.getMenu().findItem(mCurrentNavMenuId);
        if (item != null) {
            item.setChecked(false);
        }


        mCurrentNavMenuId = menuId;
        mNavView.getMenu().findItem(mCurrentNavMenuId).setChecked(true);

        Fragment fragment = mFragmentList[tag];
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        if (!mIsRestored) {
            transaction.add(R.id.fragment_content, fragment).commit();
        }

        mCurrentFragment = fragment;

    }

    public void switchFragment(int tag) {
        int menuId = mTab2MenuId.get(tag);

        // When init current menu id is 0. so here must check if MenuItem is null
        MenuItem item = mNavView.getMenu().findItem(mCurrentNavMenuId);
        if (item != null) {
            item.setChecked(false);
        }


        mCurrentNavMenuId = menuId;
        mNavView.getMenu().findItem(mCurrentNavMenuId).setChecked(true);

        Fragment fragment = mFragmentList[tag];


        /*
         * Here we have to go through this detach, replace, attach, and addToBackStack way,
         * to solve the ChildFragmentManager/ViewPager bug of Android
         * https://code.google.com/p/android/issues/detail?id=42601
         */
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        boolean mUseHideAndShow = true;

        if (mCurrentFragment == null) {
            transaction.add(R.id.fragment_content, fragment).commit();

        } else {
            if (!fragment.isAdded()) {
                transaction.hide(mCurrentFragment).add(R.id.fragment_content, fragment).commit();
                stopLiveView();
            } else {
                transaction.hide(mCurrentFragment).show(fragment).commit();
                stopLiveView();
                startLiveView(fragment);
            }


        }
        mCurrentFragment = fragment;

    }

    /**
     * The following 2 methods do something trick, because we use fragment hide/show,
     * instead of replace/backStack.
     */
    private void stopLiveView() {
        if (mCurrentFragment instanceof CameraPreviewFragment) {
            ((CameraPreviewFragment) mCurrentFragment).stopPreview();
        }
    }

    private void startLiveView(Fragment fragment) {
        if (fragment instanceof CameraPreviewFragment) {
            ((CameraPreviewFragment) fragment).startPreview();
        }
    }

    private void setupNavigationView() {
        mUserAvatar = (CircleImageView) mNavView.getHeaderView(0).findViewById(R.id.civUserAvatar);
        mUsername = (TextView) mNavView.getHeaderView(0).findViewById(R.id.tvUserName);
        mNavView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                switch (item.getItemId()) {

//                  case R.id.changeTheme:
//                       onToggleAppThemeClicked();
//
//                       break;
                    default:
                        mDrawerLayout.closeDrawers();
                        if (item.getItemId() == mCurrentNavMenuId) {
                            return true;
                        }
                        int tab = mMenuId2Tab.get(item.getItemId());
                        switchFragment(tab);
                }

                return true;
            }


        });

        mUserAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onUserAvatarClicked();
            }
        });
    }

    private void refressNavHeaderView() {
        if (mSessionManager.isLoggedIn()) {
            mUsername.setText(mSessionManager.getUserName());

            Glide.with(this)
                .load(mSessionManager.getAvatarUrl())
                .asBitmap()
                .placeholder(R.drawable.menu_profile_photo_default)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true)
                .into(mUserAvatar);
        } else {
            mUsername.setText(getText(R.string.click_2_login));

            mUserAvatar.setImageResource(R.drawable.menu_profile_photo_default);
        }
    }

    private void onUserAvatarClicked() {
        if (mSessionManager.isLoggedIn()) {
//            UserProfileActivity.launch(this, mSessionManager.getUserId(), mUserAvatar);
            AccountActivity.launch(this);
        } else {
            AuthorizeActivity.launch(this);
        }
    }

    private void onToggleAppThemeClicked() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(getText(R.string.change_theme_hint))
            .negativeText(android.R.string.cancel)
            .positiveText(android.R.string.ok)
            .callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog dialog) {
                    super.onPositive(dialog);
                    toggleAppTheme();
                    finish();
                }
            })
            .show();

    }

    public void showDrawer() {
        mDrawerLayout.openDrawer(Gravity.LEFT);
    }

    private void toggleAppTheme() {
        String appTheme = PreferenceUtils.getString(PreferenceUtils.APP_THEME, "dark");
        if (appTheme.equals("dark")) {
            getApplication().setTheme(R.style.LightTheme);
            PreferenceUtils.putString(PreferenceUtils.APP_THEME, "light");

        } else {
            getApplication().setTheme(R.style.DarkTheme);
            PreferenceUtils.putString(PreferenceUtils.APP_THEME, "dark");
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_UP_FROM_MOMENTS:
                if (resultCode == RESULT_OK && (mCurrentFragment instanceof CommunityFragment)) {
                    Logger.t(TAG).e("test", "notifyDateChanged");
                    ((CommunityFragment) mCurrentFragment).notifyDateChanged();
                }
                break;
            case SearchView.SPEECH_REQUEST_CODE:
                if (resultCode == RESULT_OK && (mCurrentFragment instanceof CommunityFragment)) {
                    mCurrentFragment.onActivityResult(requestCode, resultCode, data);
                }
                break;


            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            return;
        }
        Fragment fragment = getFragmentManager().findFragmentById(R.id.root_container);
        if (fragment instanceof FragmentNavigator
            && ((FragmentNavigator) fragment).onInterceptBackPressed()) {
            return;
        }


        fragment = getFragmentManager().findFragmentById(R.id.fragment_content);
        if (fragment instanceof FragmentNavigator
            && ((FragmentNavigator) fragment).onInterceptBackPressed()) {

            return;
        }


        if (mReturnSnackBar != null && mReturnSnackBar.isShown()) {
            super.onBackPressed();
        } else {
            mReturnSnackBar = Snackbar.make(mDrawerLayout, getText(R.string.backpressed_hint), Snackbar.LENGTH_LONG);
            mReturnSnackBar.show();
        }


    }
}
