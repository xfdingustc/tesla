package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.eventbus.events.ClipSelectEvent;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.ui.clips.cliptrimmer.ClipSetProgressBar;
import com.waylens.hachi.ui.clips.enhance.EnhanceActivity;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.settings.myvideo.DownloadVideoActivity;
import com.waylens.hachi.utils.TransitionHelper;
import com.xfdingustc.snipe.SnipeError;
import com.xfdingustc.snipe.VdbRequest;
import com.xfdingustc.snipe.VdbRequestQueue;
import com.xfdingustc.snipe.VdbResponse;
import com.xfdingustc.snipe.toolbox.AddBookmarkRequest;
import com.xfdingustc.snipe.toolbox.ClipDeleteRequest;
import com.xfdingustc.snipe.toolbox.ClipSetExRequest;
import com.xfdingustc.snipe.toolbox.VdbImageRequest;
import com.xfdingustc.snipe.utils.DateTime;
import com.xfdingustc.snipe.vdb.Clip;
import com.xfdingustc.snipe.vdb.ClipPos;
import com.xfdingustc.snipe.vdb.ClipSet;
import com.xfdingustc.snipe.vdb.ClipSetManager;
import com.xfdingustc.snipe.vdb.ClipSetPos;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Subscriber;


/**
 * Created by Xiaofei on 2016/3/16.
 */
public class FootageActivity extends ClipPlayActivity {
    private static final String TAG = FootageActivity.class.getSimpleName();
    private static final String EXTRA_PLAYLIST_ID = "playListId";
    private int mPlaylistId = 0;
    private ClipSetManager mClipSetManager = ClipSetManager.getManager();

    private static final int DEFAULT_BOOKMARK_LENGTH = 30000;

    private int mDeleteClipCount = 0;

    private BottomSheetDialog mTimeLaspeBottomSheetDialog;

    private ClipSet mFootageClipSet;
    private ClipSet mBookmarkClipSet;

    private MaterialDialog mTimeLapseDialog;


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
                                .positiveText(R.string.ok)
                                .negativeText(R.string.cancel)
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
        } else {
            setupToolbar();
        }

    }


    public static void launch(Activity activity, int playlistId, View transitionView) {
        Intent intent = new Intent(activity, FootageActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        final Pair<View, String>[] pairs = TransitionHelper.createSafeTransitionParticipants(activity,
            false, new Pair<>(transitionView, activity.getString(R.string.clip_cover)));
        ActivityOptionsCompat options = ActivityOptionsCompat
            .makeSceneTransitionAnimation(activity, pairs);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();

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

        mPlaylistId = getIntent().getIntExtra(EXTRA_PLAYLIST_ID, -1);
        mPlaylistEditor = new PlayListEditor(mVdbRequestQueue, mPlaylistId);
        mPlaylistEditor.reconstruct();
        mFootageClipSet = getClipSet();
        embedVideoPlayFragment(true);
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
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.video_buffer);
        getToolbar().setNavigationIcon(R.drawable.navbar_close);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        getToolbar().getMenu().clear();
        getToolbar().inflateMenu(R.menu.menu_footage);
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.time_lapse:
                        showTimeLapseBottomSheet();
                        break;
                }
                return true;
            }
        });
    }

    private void showTimeLapseBottomSheet() {
        mTimeLaspeBottomSheetDialog = new BottomSheetDialog(this);
        mTimeLaspeBottomSheetDialog.setContentView(R.layout.bottom_sheet_time_lapse);
        mTimeLaspeBottomSheetDialog.findViewById(R.id.fifteen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doTimeLapse(15);
            }
        });

        mTimeLaspeBottomSheetDialog.findViewById(R.id.thirty).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doTimeLapse(30);
            }
        });

        mTimeLaspeBottomSheetDialog.findViewById(R.id.sixty).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doTimeLapse(60);
            }
        });

        mTimeLaspeBottomSheetDialog.findViewById(R.id.onehundredandtwenty).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doTimeLapse(120);
            }
        });
        mTimeLaspeBottomSheetDialog.show();
    }

    private void doTimeLapse(int speed) {
        Logger.t(TAG).d("clip start time: " + getClipSet().getClip(0).getStartTimeMs());
        BgJobHelper.timeLapse(getClipSet().getClip(0), speed);
        mTimeLaspeBottomSheetDialog.dismiss();
        DownloadVideoActivity.launch(FootageActivity.this);
    }

    private void setupClipProgressBar() {
        mClipSetProgressBar.setClipSet(mFootageClipSet, mBookmarkClipSet);
    }


    private void refreshBookmarkClipSet() {
        ClipSetExRequest request = new ClipSetExRequest(Clip.TYPE_MARKED, ClipSetExRequest
            .FLAG_CLIP_EXTRA, new VdbResponse.Listener<ClipSet>() {
            @Override
            public void onResponse(ClipSet response) {
                mBookmarkClipSet = response;
                setupClipProgressBar();
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

    @Override
    protected ClipSet getClipSet() {
        return mClipSetManager.getClipSet(mPlaylistId);
    }

    private void toEnhance(List<Clip> clipList) {
//        EnhancementActivity.launch(this, (ArrayList<Clip>)clipList, EnhancementActivity.LAUNCH_MODE_ENHANCE);
        final int playlistId = 0x100;
        PlayListEditor playListEditor = new PlayListEditor(mVdbRequestQueue, playlistId);

        playListEditor.buildRx(clipList)
            .subscribe(new Subscriber<Void>() {
                @Override
                public void onCompleted() {
                    EnhanceActivity.launch(FootageActivity.this, playlistId);
                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Void aVoid) {

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
