package com.waylens.hachi.ui.clips.preview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.clips.ClipModifyActivity;
import com.waylens.hachi.ui.clips.ClipPlayActivity;
import com.waylens.hachi.ui.clips.enhance.EnhanceActivity;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.clips.share.ShareActivity;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.TransitionHelper;
import com.xfdingustc.snipe.SnipeError;
import com.xfdingustc.snipe.VdbResponse;
import com.xfdingustc.snipe.toolbox.ClipDeleteRequest;
import com.xfdingustc.snipe.vdb.Clip;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class PreviewActivity extends ClipPlayActivity {
    public static String TAG = PreviewActivity.class.getSimpleName();

    private int mPlaylistId = 0;

    public static Clip mClip;

    public static final String EXTRA_PLAYLIST_ID = "playListId";


    public static void launch(Activity activity, int playlistId, View transitionView, Clip clip) {
        Intent intent = new Intent(activity, PreviewActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        intent.putExtra("clip", (Parcelable) clip);
        mClip = clip;
        Logger.t(TAG).d("type race:" + clip.typeRace);
        final Pair<View, String>[] pairs = TransitionHelper.createSafeTransitionParticipants(activity,
            false, new Pair<>(transitionView, activity.getString(R.string.clip_cover)));
        ActivityOptionsCompat options = ActivityOptionsCompat
            .makeSceneTransitionAnimation(activity, pairs);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getParcelableExtra("clip") == null) {
            mClip = null;
        }
        Logger.t(TAG).d("type race:" + mClip.typeRace);
        Logger.t(TAG).d("vin:" + mClip.getVin());
        Logger.t(TAG).d("duration:" + mClip.getDurationMs());
        init();
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_preview);
        setupToolbar();

        mPlaylistId = getIntent().getIntExtra(EXTRA_PLAYLIST_ID, -1);
        mPlaylistEditor = new PlayListEditor(mVdbRequestQueue, mPlaylistId);
        mPlaylistEditor.reconstruct();
        embedVideoPlayFragment(true);
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.preview);
        getToolbar().inflateMenu(R.menu.menu_clip_play_fragment);
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_to_share:
                        if (!SessionManager.getInstance().isLoggedIn()) {
                            AuthorizeActivity.launch(PreviewActivity.this);
                            return true;
                        }
                        if (!SessionManager.checkUserVerified(PreviewActivity.this)) {
                            return true;
                        }
                        Logger.t(TAG).d("typeRace" + mClip.typeRace);
                        ShareActivity.launch(PreviewActivity.this, mPlaylistEditor.getPlaylistId(), -1, mClip);
                        finish();

                        break;
                    case R.id.menu_to_enhance:
                        EnhanceActivity.launch(PreviewActivity.this, mPlaylistEditor.getPlaylistId());
                        finish();
                        break;
                    case R.id.menu_to_modify:
                        ClipModifyActivity.launch(PreviewActivity.this, getClipSet().getClip(0));
//                        finish();
                        break;
                    case R.id.menu_to_delete:
                        confirmDeleteClip();
                        break;
                }

                return true;
            }
        });
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private boolean verifyLogin() {
        SessionManager sessionManager = SessionManager.getInstance();
        if (!sessionManager.isLoggedIn()) {
            AuthorizeActivity.launch(this);
            return false;
        }

        return true;
    }

    private void confirmDeleteClip() {
        DialogHelper.showDeleteHighlightConfirmDialog(this, new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                doDeleteClip();
            }
        });
    }


    private void doDeleteClip() {
        if (getClipSet() == null || getClipSet().getClip(0) == null) {
            return;
        }

        ClipDeleteRequest request = new ClipDeleteRequest(getClipSet().getClip(0).cid, new VdbResponse.Listener<Integer>() {
            @Override
            public void onResponse(Integer response) {
                PreferenceUtils.putBoolean(PreferenceUtils.BOOKMARK_NEED_REFRESH, true);
                finish();
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
    }

}
