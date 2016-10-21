package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.WindowManager;

import com.lapism.searchview.SearchView;
import com.orhanobut.logger.Logger;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;
import com.waylens.hachi.R;
import com.waylens.hachi.gcm.RegistrationIntentService;
import com.waylens.hachi.ui.clips.ClipVideoFragment;
import com.waylens.hachi.ui.community.CommunityFragment;
import com.waylens.hachi.ui.community.PerformanceTestFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.liveview.CameraPreviewFragment;
import com.waylens.hachi.ui.settings.AccountFragment;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/1/15.
 */
public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
//    private ActionBarDrawerToggle mDrawerToggle;

    public static final int TAB_TAG_VIDEO = 0;
    public static final int TAB_TAG_LIVE_VIEW = 1;
    public static final int TAB_TAG_MOMENTS = 2;
    public static final int TAB_TAG_ACCOUNT = 3;
    public static final int TAB_TAG_LEADERBOARD = 4;

    public static final int REQUEST_CODE_SIGN_UP_FROM_MOMENTS = 100;


    private Map<Integer, Integer> mTab2MenuId = new HashMap<>();

    private Fragment[] mFragmentList = new Fragment[]{
        new ClipVideoFragment(),
        new CameraPreviewFragment(),
        new CommunityFragment(),
        new AccountFragment(),
//        new PerformanceTestFragment(),
    };

    private Fragment mCurrentFragment = null;


    @BindView(R.id.bottomBar)
    BottomBar mBottomBar;


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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            hideSystemUI(true);
            mBottomBar.setVisibility(View.GONE);
        } else {
            hideSystemUI(false);
            mBottomBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void init() {
        super.init();

        mTab2MenuId.put(TAB_TAG_MOMENTS, R.id.moments);
        mTab2MenuId.put(TAB_TAG_ACCOUNT, R.id.setting);
        mTab2MenuId.put(TAB_TAG_VIDEO, R.id.video);
        mTab2MenuId.put(TAB_TAG_LIVE_VIEW, R.id.live_view);
//        mTab2MenuId.put(TAB_TAG_LEADERBOARD, R.id.leaderboard);


        initViews();

        RegistrationIntentService.launch(this);

//        if (VdtCameraManager.getManager().isConnected()) {
//            initFragment(TAB_TAG_LIVE_VIEW);
//        } else {
//            initFragment(TAB_TAG_MOMENTS);
//        }
    }


    private void initViews() {
        setContentView(R.layout.activity_main);
        mBottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                switch (tabId) {
                    case R.id.moments:
                        switchFragment(TAB_TAG_MOMENTS);
                        break;
                    case R.id.account:
                        switchFragment(TAB_TAG_ACCOUNT);
                        break;
                    case R.id.video:
                        switchFragment(TAB_TAG_VIDEO);
                        break;
                    case R.id.live_view:
                        switchFragment(TAB_TAG_LIVE_VIEW);
                        break;
//                    case R.id.leaderboard:
//                        switchFragment(TAB_TAG_LEADERBOARD);
//                        break;
                }
            }
        });

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


    public void switchFragment(int tag) {
        int menuId = mTab2MenuId.get(tag);

//        mNavView.getMenu().findItem(mCurrentNavMenuId).setChecked(true);

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
            mReturnSnackBar = Snackbar.make(mBottomBar, getText(R.string.backpressed_hint), Snackbar.LENGTH_LONG);
            mReturnSnackBar.show();
        }
    }
}
