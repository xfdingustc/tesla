package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.eventbus.events.ClipSelectEvent;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.library.snipe.SnipeError;
import com.waylens.hachi.library.snipe.VdbRequest;
import com.waylens.hachi.library.snipe.VdbRequestQueue;
import com.waylens.hachi.library.snipe.VdbResponse;
import com.waylens.hachi.library.snipe.toolbox.AddBookmarkRequest;
import com.waylens.hachi.library.snipe.toolbox.ClipDeleteRequest;
import com.waylens.hachi.library.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.library.snipe.toolbox.VdbImageRequest;
import com.waylens.hachi.library.utils.DateTime;
import com.waylens.hachi.library.vdb.Clip;
import com.waylens.hachi.library.vdb.ClipPos;
import com.waylens.hachi.library.vdb.ClipSet;
import com.waylens.hachi.library.vdb.ClipSetManager;
import com.waylens.hachi.library.vdb.ClipSetPos;
import com.waylens.hachi.ui.clips.cliptrimmer.ClipSetProgressBar;
import com.waylens.hachi.ui.clips.enhance.EnhanceActivity;
import com.waylens.hachi.ui.clips.player.ClipPlayFragment;
import com.waylens.hachi.ui.clips.player.PlaylistUrlProvider;
import com.waylens.hachi.ui.clips.player.UrlProvider;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;


/**
 * Created by Xiaofei on 2016/3/16.
 */
public class FootageActivity extends ClipPlayActivity {
    private static final String TAG = FootageActivity.class.getSimpleName();

    private ClipPlayFragment mClipPlayFragment;

    private ClipSetManager mClipSetManager = ClipSetManager.getManager();

    private static final int DEFAULT_BOOKMARK_LENGTH = 30000;


    private static final String CLIPSET_INDEX = "clipsetindex";

    private int mClipSetIndex;

    private int mDeleteClipCount = 0;


    private ClipSet mFootageClipSet;
    private ClipSet mBookmarkClipSet;

    private PlayListEditor mPlaylistEditor;


    @BindView(R.id.vsRoot)
    View mRootView;

    @BindView(R.id.clipSetPrgressBar)
    ClipSetProgressBar mClipSetProgressBar;

    @BindView(R.id.btnAddBookmark)
    ImageButton mBtnAddBookmark;

    @BindView(R.id.tvClipPosTime)
    TextView mTvClipPosTime;

    @OnClick(R.id.btnAddBookmark)
    public void onBtnAddBookmarkClicked() {
        doAddBookmark();

    }

    @Subscribe
    public void onEventCameraConnection(CameraConnectionEvent event) {
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_SELECTED_CHANGED:
                initCamera();
                refreshFootageClipSet();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventClipSetPosChanged(ClipSetPosChangeEvent event) {
        ClipSetPos clipSetPos = event.getClipSetPos();
        Clip clip = getClipSet().getClip(clipSetPos.getClipIndex());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy, MMM dd, hh:mm:ss, a");

        mTvClipPosTime.setText(simpleDateFormat.format(DateTime.getTimeDate(clip.getClipDate(), clipSetPos.getClipTimeMs())));
    }

    @Subscribe
    public void onEventClipSelectEvent(final ClipSelectEvent event) {
        getToolbar().getMenu().clear();
        if (event.getClipList() != null) {
            getToolbar().inflateMenu(R.menu.menu_clip_footage);
            if (event.getClipList().size() > 1) {
                getToolbar().getMenu().removeItem(R.id.menu_to_modify);
            }
            getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menu_to_enhance:
                            toEnhance(event.getClipList());
                            break;
                        case R.id.menu_to_modify:
                            toModify(event.getClipList().get(0));
                            break;
                        case R.id.menu_to_delete:
                            MaterialDialog dialog = new MaterialDialog.Builder(FootageActivity.this)
                                .content(R.string.delete_bookmark_confirm)
                                .positiveText(android.R.string.ok)
                                .negativeText(android.R.string.cancel)
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        doDeleteSelectedClips(event.getClipList());
                                    }
                                })
                                .build();
                            dialog.show();

                            break;
                        default:
                            break;
                    }
                    return true;
                }
            });
        }

    }


    public static void launch(Activity activity, int clipSetIndex) {
        Intent intent = new Intent(activity, FootageActivity.class);
        intent.putExtra(CLIPSET_INDEX, clipSetIndex);
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
        mClipSetIndex = getIntent().getIntExtra(CLIPSET_INDEX, ClipSetManager.CLIP_SET_TYPE_ALLFOOTAGE);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_footage);
        mRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEventBus.post(new ClipSelectEvent(null));
            }
        });
        setupToolbar();
        mClipSetProgressBar.init(mVdbRequestQueue);
        refreshFootageClipSet();

    }


    @Override
    public void onStart() {
        super.onStart();
        refreshBookmarkClipSet();
        mEventBus.register(mClipSetProgressBar);
    }

    @Override
    public void onStop() {
        super.onStop();
        mEventBus.unregister(mClipSetProgressBar);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.cancelAll(new VdbRequestQueue.RequestFilter() {
                @Override
                public boolean apply(VdbRequest<?> request) {
                    if (request instanceof VdbImageRequest) {
//                        Logger.t(TAG).d("cancel image quest");
                        return true;
                    }

                    return false;
                }
            });
        }
    }


    @Override
    public void setupToolbar() {
        if (getToolbar() != null) {
            getToolbar().setTitle(R.string.footage);
            getToolbar().setNavigationIcon(R.drawable.navbar_close);
            getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
        super.setupToolbar();
    }

    private void setupClipPlayFragment(ClipSet clipSet) {
        mClipSetManager.updateClipSet(mClipSetIndex, clipSet);
        UrlProvider urlProvider1 = new PlaylistUrlProvider(mVdbRequestQueue, 0x101);

        mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, mClipSetIndex, urlProvider1,
            ClipPlayFragment.ClipMode.SINGLE, ClipPlayFragment.CoverMode.NORMAL);

        getFragmentManager().beginTransaction().add(R.id.player_fragment_content, mClipPlayFragment).commit();
        Logger.t(TAG).d("clipSet count: " + clipSet.getCount());
        doMakePlaylist(clipSet);

    }

    private void doMakePlaylist(ClipSet clipSet) {
        mPlaylistEditor = new PlayListEditor(mVdbRequestQueue, 0x101);
        mPlaylistEditor.build(clipSet.getClipList(), new PlayListEditor.OnBuildCompleteListener() {
            @Override
            public void onBuildComplete(ClipSet clipSet) {
                Logger.t(TAG).d("clipSet count: " + clipSet.getCount());
            }
        });

    }


    private void setupClipProgressBar() {
        mClipSetProgressBar.setClipSet(mFootageClipSet, mBookmarkClipSet);
    }


    private void refreshFootageClipSet() {
        if (mVdbRequestQueue == null) {
            return;
        }

        mFootageClipSet = mClipSetManager.getClipSet(mClipSetIndex);
        mClipSetManager.updateClipSet(ClipSetManager.CLIP_SET_TYPE_TMP, mFootageClipSet);

        setupClipPlayFragment(mFootageClipSet);
        //setupClipProgressBar(clipSet);


    }

    private void refreshBookmarkClipSet() {
        ClipSetExRequest request = new ClipSetExRequest(Clip.TYPE_MARKED, ClipSetExRequest
            .FLAG_CLIP_EXTRA, new VdbResponse.Listener<ClipSet>() {
            @Override
            public void onResponse(ClipSet response) {
                mBookmarkClipSet = response;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupClipProgressBar();
                    }
                });

            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
    }

    private void doAddBookmark() {
        //Clip clip = mClipSetProgressBar.getSelectClip;

        ClipPos clipPos = mClipSetProgressBar.getCurrentClipPos();
        ClipSetPos clipSetPos = mClipSetProgressBar.getCurrentClipSetPos();
        Clip clip = getClipSet().getClip(clipSetPos.getClipIndex());

        long startTimeMs = clipPos.getClipTimeMs() - mVdtCamera.getMarkBeforeTime() * 1000;
        long endTimeMs = clipPos.getClipTimeMs() + mVdtCamera.getMarkAfterTime() * 1000;


        startTimeMs = Math.max(startTimeMs, clip.getStartTimeMs());
        endTimeMs = Math.min(endTimeMs, clip.getEndTimeMs());

        AddBookmarkRequest request = new AddBookmarkRequest(clipPos.cid, startTimeMs, endTimeMs, new
            VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    refreshBookmarkClipSet();
                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });

        mVdbRequestQueue.add(request);
    }

    private ClipSet getClipSet() {
        return mClipSetManager.getClipSet(mClipSetIndex);
    }

    private void toEnhance(List<Clip> clipList) {
//        EnhancementActivity.launch(this, (ArrayList<Clip>)clipList, EnhancementActivity.LAUNCH_MODE_ENHANCE);
        final int playlistId = 0x100;
        PlayListEditor playListEditor = new PlayListEditor(mVdbRequestQueue, playlistId);
        playListEditor.build(clipList, new PlayListEditor.OnBuildCompleteListener() {
            @Override
            public void onBuildComplete(ClipSet clipSet) {
                EnhanceActivity.launch(FootageActivity.this, playlistId);
            }
        });

    }

    private void toModify(Clip clip) {
        ClipModifyActivity.launch(this, clip);
    }


    private void doDeleteSelectedClips(List<Clip> clipList) {

        final int toDeleteClipCount = clipList.size();
        mDeleteClipCount = 0;

        for (Clip clip : clipList) {
            ClipDeleteRequest request = new ClipDeleteRequest(clip.cid, new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    mDeleteClipCount++;
                    Logger.t(TAG).d("" + mDeleteClipCount + " clips deleted");
                    if (mDeleteClipCount == toDeleteClipCount) {
                        refreshBookmarkClipSet();
                    }
                }
            }, new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {

                }
            });

            mVdbRequestQueue.add(request);
        }

    }

}
