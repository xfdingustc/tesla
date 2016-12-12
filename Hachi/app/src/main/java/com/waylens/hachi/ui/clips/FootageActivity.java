package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.eventbus.events.ClipSelectEvent;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.snipe.reative.SnipeApiRx;
import com.waylens.hachi.snipe.remix.AvrproFilter;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.utils.DateTime;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipPos;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.ClipSetManager;
import com.waylens.hachi.snipe.vdb.ClipSetPos;
import com.waylens.hachi.ui.clips.cliptrimmer.ClipSetProgressBar;
import com.waylens.hachi.ui.clips.enhance.EnhanceActivity;
import com.waylens.hachi.ui.clips.player.RawDataLoader;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.ui.settings.myvideo.ExportedVideoActivity;
import com.waylens.hachi.utils.TransitionHelper;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;


import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Created by Xiaofei on 2016/3/16.
 */
public class FootageActivity extends ClipPlayActivity {
    private static final String TAG = FootageActivity.class.getSimpleName();
    private static final String EXTRA_PLAYLIST_ID = "playListId";
    private int mPlaylistId = 0;
    private ClipSetManager mClipSetManager = ClipSetManager.getManager();

    private static final int DEFAULT_BOOKMARK_LENGTH = 30000;


    private BottomSheetDialog mTimeLaspeBottomSheetDialog;

    private ClipSet mFootageClipSet;
    private ClipSet mBookmarkClipSet;
    private AvrproFilter mAvrproFilter;
    private RawDataLoader mRawDataLoader;

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
                            DialogHelper.showDeleteHighlightConfirmDialog(FootageActivity.this, new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    doDeleteSelectedClips(event.getClipList());
                                }
                            });

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
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pairs);
//        ActivityCompat.startActivity(activity, intent, options.toBundle());
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
    protected void onDestroy() {
        super.onDestroy();
        mPlaylistEditor.clearRx()
            .subscribe(new SimpleSubscribe<Void>() {
                @Override
                public void onNext(Void aVoid) {

                }
            });
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.video_buffer);
        getToolbar().setNavigationIcon(R.drawable.ic_close);
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
        ExportedVideoActivity.launch(FootageActivity.this);
    }

    private void setupClipProgressBar() {
        mClipSetProgressBar.setClipSet(mFootageClipSet, mBookmarkClipSet);
    }


    private void refreshBookmarkClipSet() {
        SnipeApiRx.getClipSetRx(Clip.TYPE_MARKED, ClipSetExRequest.FLAG_CLIP_EXTRA)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<ClipSet>() {
                @Override
                public void onNext(ClipSet clipSet) {
                    mBookmarkClipSet = clipSet;
                    setupClipProgressBar();
                }
            });
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


        SnipeApiRx.addHighlightRx(clipPos.cid, startTimeMs, endTimeMs)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<Integer>() {
                @Override
                public void onNext(Integer integer) {
                    refreshBookmarkClipSet();
                }
            });
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
        SnipeApiRx.deleteClipListRx(clipList)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Integer>() {
                @Override
                public void onCompleted() {
                    refreshBookmarkClipSet();
                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Integer integer) {
                }
            });

    }

}
