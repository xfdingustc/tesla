package com.waylens.hachi.ui.clips.preview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipDeleteRequest;
import com.waylens.hachi.snipe.toolbox.ClipInfoRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.rawdata.GpsData;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.clips.ClipModifyActivity;
import com.waylens.hachi.ui.clips.ClipPlayActivity;
import com.waylens.hachi.ui.clips.enhance.EnhanceActivity;
import com.waylens.hachi.ui.clips.player.RawDataLoader;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.clips.share.ShareActivity;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.StatusBarHelper;
import com.waylens.hachi.utils.TransitionHelper;

import java.util.ArrayList;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class PreviewActivity extends ClipPlayActivity {
    public static String TAG = PreviewActivity.class.getSimpleName();

    private int mPlaylistId = 0;

    private ArrayList<Long> mTimeList = null;

    private String mVin;

    private int raceType = -1;

    public static final String EXTRA_PLAYLIST_ID = "playListId";


    public static void launch(Activity activity, int playlistId, View transitionView) {
        Intent intent = new Intent(activity, PreviewActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        final Pair<View, String>[] pairs = TransitionHelper.createSafeTransitionParticipants(activity,
            false, new Pair<>(transitionView, activity.getString(R.string.clip_cover)));
        ActivityOptionsCompat options = ActivityOptionsCompat
            .makeSceneTransitionAnimation(activity, pairs);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        getRaceTimeInfo();
    }

    public void getRaceTimeInfo() {
        Observable.create(new Observable.OnSubscribe<ArrayList<Long>>() {
            @Override
            public void call(final Subscriber<? super ArrayList<Long>> subscriber) {
                Logger.t(TAG).d("start loading ");
                // First load raw data into memory
                RawDataLoader mRawDataLoader = new RawDataLoader(mPlaylistId, mVdbRequestQueue);
                mRawDataLoader.loadRawData();

                ClipSet clipSet = getClipSet();
                ArrayList<Clip> clipList;
                if (clipSet != null) {
                    clipList = clipSet.getClipList();
                } else {
                    Logger.t(TAG).d("clipset empty!");
                    return;
                }

                String vin = null;
                for (Clip clip : clipList) {
                    Logger.t(TAG).d("Vin  = " + clip.getVin());
                    if (clip.getVin() != null) {
                        vin = clip.getVin();
                        mVin = vin;
                    }
                    Logger.t(TAG).d("clip" + clip.cid.type);
                    Clip retClip = loadClipInfo(clip);
                    Logger.t(TAG).d("typeRace:" + retClip.typeRace);
                    if ((retClip.typeRace & Clip.TYPE_RACE) > 0) {
                        raceType = retClip.typeRace;
                        Logger.t(TAG).d("duration:" + retClip.getDurationMs());
                        Logger.t(TAG).d(retClip.typeRace & Clip.MASK_RACE);
                        Logger.t(TAG).d("t1:" + retClip.raceTimingPoints.get(0));
                        Logger.t(TAG).d("t2:" + retClip.raceTimingPoints.get(1));
                        Logger.t(TAG).d("t3:" + retClip.raceTimingPoints.get(2));
                        Logger.t(TAG).d("t4:" + retClip.raceTimingPoints.get(3));
                        Logger.t(TAG).d("t5:" + retClip.raceTimingPoints.get(4));
                        Logger.t(TAG).d("t6:" + retClip.raceTimingPoints.get(5));

                        Logger.t(TAG).d("start loading ");
                        // First load raw data into memory
                        RawDataBlock rawDataBlock = mRawDataLoader.loadRawData(clip, RawDataItem.DATA_TYPE_GPS);
                        Logger.t(TAG).d("raw data size:" + rawDataBlock.getItemList().size());
 /*                       GpsData firstGpsData = (GpsData) rawDataBlock.getItemList().get(0).data;
                        if (((long) firstGpsData.utc_time * 1000 + firstGpsData.reserved / 1000) >= clip.raceTimingPoints.get(0)) {
                            Logger.t(TAG).d("find the corresponding video time:" + rawDataBlock.getItemList().get(0).getPtsMs());
                            continue;
                        }*/
                        int searchIndex = -1;
                        long searchResult = -1;
                        if ((retClip.typeRace & Clip.MASK_RACE) == Clip.TYPE_RACE_CD3T || (retClip.typeRace & Clip.MASK_RACE) == Clip.TYPE_RACE_CD6T) {
                            searchIndex = 0;
                        } else {
                            searchIndex = 1;
                        }
                        ArrayList<Long> timeList = new ArrayList<Long>(6);
                        long clipStartTime;
                        for (int i = 1; i < rawDataBlock.getItemList().size(); i++) {
                            RawDataItem last = rawDataBlock.getItemList().get(i - 1);
                            RawDataItem current = rawDataBlock.getItemList().get(i);
                            GpsData lastGpsData = (GpsData) last.data;
                            GpsData currentGpsData = (GpsData) current.data;
                            long lastGpsTime = (long) lastGpsData.utc_time * 1000 + lastGpsData.reserved / 1000;
                            long currentGpsTime = (long) currentGpsData.utc_time * 1000 + currentGpsData.reserved / 1000;
                            if (lastGpsTime <= retClip.raceTimingPoints.get(searchIndex) && currentGpsTime >= retClip.raceTimingPoints.get(searchIndex)) {
                                if (2 * retClip.raceTimingPoints.get(searchIndex) <= lastGpsTime + currentGpsTime) {
                                    Logger.t(TAG).d("gps utc time ms:" + ((long) lastGpsData.utc_time * 1000 + lastGpsData.reserved / 1000));
                                    Logger.t(TAG).d("find the corresponding video time:" + last.getPtsMs());
                                    searchResult = last.getPtsMs() + retClip.getClipDate();
                                } else {
                                    Logger.t(TAG).d("gps utc time ms:" + ((long) currentGpsData.utc_time * 1000 + currentGpsData.reserved / 1000));
                                    Logger.t(TAG).d("find the corresponding video time:" + current.getPtsMs());
                                    searchResult = current.getPtsMs() + retClip.getClipDate();
                                }
                                clipStartTime = retClip.getStartTimeMs() + retClip.getClipDate();
                                if (searchIndex == 0) {
                                    timeList.add(0, searchResult);
                                    timeList.add(1, retClip.raceTimingPoints.get(1) - retClip.raceTimingPoints.get(0) + searchResult);
                                    timeList.add(2, retClip.raceTimingPoints.get(2) - retClip.raceTimingPoints.get(0) + searchResult);
                                    timeList.add(3, retClip.raceTimingPoints.get(3) - retClip.raceTimingPoints.get(0) + searchResult);
                                    if (retClip.raceTimingPoints.get(4) > 0) {
                                        timeList.add(4, retClip.raceTimingPoints.get(4) - retClip.raceTimingPoints.get(0) + searchResult);
                                    } else {
                                        timeList.add(4, (long) -1);
                                    }
                                    if (retClip.raceTimingPoints.get(5) > 0) {
                                        timeList.add(5, retClip.raceTimingPoints.get(5) - retClip.raceTimingPoints.get(0) + searchResult);
                                    } else {
                                        timeList.add(5, (long) -1);
                                    }
                                } else if (searchIndex == 1) {
                                    timeList.add(0, (long) -1);
                                    timeList.add(1, searchResult);
                                    timeList.add(2, retClip.raceTimingPoints.get(2) - retClip.raceTimingPoints.get(1) + searchResult);
                                    timeList.add(3, retClip.raceTimingPoints.get(3) - retClip.raceTimingPoints.get(1) + searchResult);
                                    if (retClip.raceTimingPoints.get(4) > 0) {
                                        timeList.add(4, retClip.raceTimingPoints.get(4) - retClip.raceTimingPoints.get(1) + searchResult);
                                    } else {
                                        timeList.add(4, (long) -1);
                                    }
                                    if (retClip.raceTimingPoints.get(5) > 0) {
                                        timeList.add(5, retClip.raceTimingPoints.get(5) - retClip.raceTimingPoints.get(1) + searchResult);
                                    } else {
                                        timeList.add(5, (long) -1);
                                    }
                                }
                                for (int j = 0; j < timeList.size(); j++) {
                                    timeList.set(j, timeList.get(j) - clipStartTime);
                                }
                                mTimeList = new ArrayList<Long>(6);
                                for (int j = 0; j < timeList.size(); j++) {
                                    if (timeList.get(j) > 0) {
                                        mTimeList.add(j, timeList.get(j));
                                    } else {
                                        mTimeList.add(j, (long) -1);
                                    }
                                }
                                subscriber.onNext(timeList);
                                break;
                            }
                            //Logger.t(TAG).d("utc time:" + currentGpsData.utc_time);
                            //Logger.t(TAG).d("reserve utc time:" + currentGpsData.reserved);
                        }
                    }
                }

                subscriber.onCompleted();
            }
        }).observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(new Observer<ArrayList<Long>>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(ArrayList<Long> timeList) {
                    Logger.t(TAG).d("set time Points");
                    if (timeList.size() > 0) {
                        mClipPlayFragment.setRaceTimePoints(timeList);
                    }

                }
            });

    }

    private Clip loadClipInfo(Clip clip) {
        VdbRequestFuture<Clip> requestFuture = VdbRequestFuture.newFuture();
        ClipInfoRequest request = new ClipInfoRequest(clip.cid, ClipSetExRequest.FLAG_CLIP_EXTRA | ClipSetExRequest.FLAG_CLIP_DESC | ClipSetExRequest.FLAG_CLIP_SCENE_DATA,
            clip.cid.type, 0, requestFuture, requestFuture);
        mVdbRequestQueue.add(request);
        try {
            Clip retClip = requestFuture.get();
            return retClip;
        } catch (Exception e) {
            Logger.t(TAG).e("Load raw data: " + e.getMessage());
            return null;
        }
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
                        ShareActivity.launch(PreviewActivity.this, mPlaylistEditor.getPlaylistId(), -1, mVin, mTimeList, raceType);
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
        StatusBarHelper.setStatusBarColor(this, Color.BLACK);
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
