package com.waylens.hachi.ui.clips;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.NonNull;
import android.transition.ChangeBounds;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.camera.events.CameraConnectionEvent;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.ClipSetManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.clips.player.ClipPlayFragment;
import com.waylens.hachi.ui.clips.player.PlaylistUrlProvider;
import com.waylens.hachi.ui.clips.player.UrlProvider;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class ClipPlayActivity extends BaseActivity {
    private static final String TAG = ClipPlayActivity.class.getSimpleName();

    @BindView(R.id.player_fragment_content)
    public FrameLayout mPlayerContainer;

    private int mOriginalTopMargin;
    private int mOriginalHeight;

    protected ClipPlayFragment mClipPlayFragment;

    protected PlayListEditor mPlaylistEditor;
    protected EventBus mEventBus = EventBus.getDefault();


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCameraConnectChange(CameraConnectionEvent event) {
        if (event.getWhat() == CameraConnectionEvent.VDT_CAMERA_DISCONNECTED) {
            initCamera();
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content(R.string.camera_disconnected)
                .positiveText(R.string.quit)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();
                    }
                })
                .show();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mEventBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mEventBus.unregister(this);
        if (mPlaylistEditor != null) {
            mPlaylistEditor.cancel();
        }
    }


    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mPlayerContainer.getLayoutParams();
        mOriginalTopMargin = layoutParams.topMargin;
        mOriginalHeight = layoutParams.height;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mPlayerContainer.getLayoutParams();
        Logger.t(TAG).d("newConfig.orientation " + newConfig.orientation);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getToolbar().setVisibility(View.GONE);
            layoutParams.topMargin = 0;
            layoutParams.width = ViewGroup.MarginLayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.MarginLayoutParams.MATCH_PARENT;
            mPlayerContainer.setLayoutParams(layoutParams);
        } else {
            getToolbar().setVisibility(View.VISIBLE);
            layoutParams.width = ViewGroup.MarginLayoutParams.MATCH_PARENT;
            layoutParams.height = mOriginalHeight;

        }

    }


    @Override
    public void onBackPressed() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setImmersiveMode(false);
        } else {
            mClipPlayFragment.onBackPressed();
            super.onBackPressed();
        }
    }

    protected void embedVideoPlayFragment() {

        embedVideoPlayFragment(false);
    }

    protected void embedVideoPlayFragment(boolean transition) {

        UrlProvider vdtUriProvider = new PlaylistUrlProvider(mPlaylistEditor.getPlaylistId());

        mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, mPlaylistEditor.getPlaylistId(),
            vdtUriProvider, ClipPlayFragment.ClipMode.MULTI);

        if (transition) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mClipPlayFragment.setSharedElementEnterTransition(new ChangeBounds());
            }
        }

        getFragmentManager().beginTransaction().replace(R.id.player_fragment_content, mClipPlayFragment).commit();
    }

    protected ClipSet getClipSet() {
        return ClipSetManager.getManager().getClipSet(mPlaylistEditor.getPlaylistId());
    }
}
