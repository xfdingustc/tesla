package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.fragments.EnhanceFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.ShareFragment;
import com.waylens.hachi.ui.fragments.clipplay.VideoPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.PlaylistUrlProvider;
import com.waylens.hachi.ui.fragments.clipplay2.UrlProvider;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;

import java.util.ArrayList;

import butterknife.Bind;

/**
 * Created by Richard on 12/18/15.
 */
public class EnhancementActivity extends BaseActivity implements FragmentNavigator,
        android.support.v7.widget.Toolbar.OnMenuItemClickListener,
        ClipPlayFragment.ClipPlayFragmentContainer {
    private static final String TAG = EnhancementActivity.class.getSimpleName();
    public static final int REQUEST_CODE_SIGN_UP_FROM_ENHANCE = 200;

    public static final String EXTRA_CLIPS_TO_ENHANCE = "extra.clips.to.enhance";
    public static final String EXTRA_CLIPS_TO_APPEND = "extra.clips.to.append";
    public static final String EXTRA_LAUNCH_MODE = "extra.launch.mode";

    public static final int LAUNCH_MODE_QUICK_VIEW = 0;
    public static final int LAUNCH_MODE_ENHANCE = 1;
    public static final int LAUNCH_MODE_SHARE = 2;
    public static final int LAUNCH_MODE_MODIFY = 3;

    private ClipPlayFragment mClipPlayFragment;

    @Bind(R.id.top_place_holder)
    View mTopPlaceHolder;

    int mLaunchMode;

    private VdbRequestQueue mVdbRequestQueue;
    private VdtCamera mVdtCamera;

    EnhanceFragment mEnhanceFragment;
    ShareFragment mShareFragment;

    public static void launch(Activity activity, ArrayList<Clip> clipList, int launchMode) {
        Intent intent = new Intent(activity, EnhancementActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_CLIPS_TO_ENHANCE, clipList);
        intent.putExtra(EXTRA_LAUNCH_MODE, launchMode);
        activity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mVdbRequestQueue = Snipe.newRequestQueue();
        mLaunchMode = getIntent().getIntExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_QUICK_VIEW);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_enhance);
        ArrayList<Clip> mClipList = getIntent().getParcelableArrayListExtra(EXTRA_CLIPS_TO_ENHANCE);

        ClipSet clipSet = new ClipSet(Clip.TYPE_TEMP);
        ClipSet clipSetEditing = new ClipSet(Clip.TYPE_TEMP);
        for (Clip clip : mClipList) {
            clipSet.addClip(clip);
            clipSetEditing.addClip(clip);
        }

        ClipSetManager.getManager().updateClipSet(ClipSetManager.CLIP_SET_TYPE_ENHANCE, clipSet);
        ClipSetManager.getManager().updateClipSet(ClipSetManager.CLIP_SET_TYPE_ENHANCE_EDITING, clipSetEditing);
        embedVideoPlayFragment();
        switchToMode();
    }

    void switchToMode() {
        switch (mLaunchMode) {
            case LAUNCH_MODE_QUICK_VIEW:
                mTopPlaceHolder.setVisibility(View.VISIBLE);
                break;
            case LAUNCH_MODE_ENHANCE:
                mTopPlaceHolder.setVisibility(View.GONE);
                if (mEnhanceFragment == null) {
                    mEnhanceFragment = new EnhanceFragment();
                }
                switchFragment(mShareFragment, mEnhanceFragment);
                break;
            case LAUNCH_MODE_SHARE:
                if (!SessionManager.getInstance().isLoggedIn()) {
                    LoginActivity.launchForResult(this, REQUEST_CODE_SIGN_UP_FROM_ENHANCE);
                    return;
                } else {
                    mTopPlaceHolder.setVisibility(View.GONE);
                    if (mShareFragment == null) {
                        mShareFragment = new ShareFragment();
                    }
                    switchFragment(mEnhanceFragment, mShareFragment);
                }
                break;
        }
        setupToolbarImpl();
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        if (mToolbar == null) {
            return;
        }
        mToolbar.setNavigationIcon(R.drawable.navbar_close);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mToolbar.setOnMenuItemClickListener(this);
    }

    void setupToolbarImpl() {
        if (mToolbar == null) {
            return;
        }

        mToolbar.getMenu().clear();
        switch (mLaunchMode) {
            case LAUNCH_MODE_QUICK_VIEW:
                mToolbar.setTitle("");
                mToolbar.inflateMenu(R.menu.menu_clip_play_fragment);
                break;
            case LAUNCH_MODE_ENHANCE:
                mToolbar.setTitle(R.string.enhance);
                mToolbar.inflateMenu(R.menu.menu_enhance);
                break;
            case LAUNCH_MODE_SHARE:
                mToolbar.setTitle(R.string.share);
                mToolbar.inflateMenu(R.menu.menu_share);
                break;
        }
    }

    private void embedVideoPlayFragment() {
        ClipPlayFragment.Config config = new ClipPlayFragment.Config();
        config.clipMode = ClipPlayFragment.Config.ClipMode.MULTI;

        UrlProvider vdtUriProvider = new PlaylistUrlProvider(mVdbRequestQueue, 0x100);
        mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, ClipSetManager.CLIP_SET_TYPE_ENHANCE,
                vdtUriProvider,
                config);
        mClipPlayFragment.setShowsDialog(false);
        getFragmentManager().beginTransaction().replace(R.id.player_fragment_content, mClipPlayFragment).commit();
    }

    @Override
    public boolean onInterceptBackPressed() {
        if (VideoPlayFragment.fullScreenPlayer != null) {
            VideoPlayFragment.fullScreenPlayer.setFullScreen(false);
            return true;
        }
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_to_enhance:
                mLaunchMode = LAUNCH_MODE_ENHANCE;
                break;
            case R.id.menu_to_share:
                mLaunchMode = LAUNCH_MODE_SHARE;
                break;
            case R.id.menu_to_modify:
                mLaunchMode = LAUNCH_MODE_MODIFY;
                //TODO
                break;
            case R.id.menu_to_delete:
                //TODO
                break;
            case R.id.menu_to_download:
                //TODO
                break;
        }
        switchToMode();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_UP_FROM_ENHANCE:
                if (resultCode == RESULT_OK) {
                    mLaunchMode = LAUNCH_MODE_SHARE;
                    switchToMode();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    void switchFragment(Fragment currentFragment, Fragment newFragment) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (currentFragment != null) {
            ft.hide(currentFragment);
        }
        if (newFragment.isAdded()) {
            ft.show(newFragment);
        } else {
            ft.add(R.id.enhance_fragment_content, newFragment);
        }
        ft.commit();
    }

    @Override
    public ClipPlayFragment getClipPlayFragment() {
        return mClipPlayFragment;
    }

    public int getAudioID() {
        if (mEnhanceFragment != null) {
            return mEnhanceFragment.getAudioID();
        } else {
            return EnhanceFragment.DEFAULT_AUDIO_ID;
        }
    }

    public String getGaugeSettings() {
        if (mEnhanceFragment != null) {
            return mEnhanceFragment.getGaugeSettings();
        } else {
            return "";
        }
    }
}
