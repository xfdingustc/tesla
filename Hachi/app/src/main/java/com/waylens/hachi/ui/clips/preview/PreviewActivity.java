package com.waylens.hachi.ui.clips.preview;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipDeleteRequest;
import com.waylens.hachi.snipe.toolbox.ClipInfoRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.utils.RaceTimeParseUtils;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.rawdata.GpsData;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.clips.ClipModifyActivity;
import com.waylens.hachi.ui.clips.ClipPlayActivity;
import com.waylens.hachi.ui.clips.TranscodingActivity;
import com.waylens.hachi.ui.clips.enhance.EnhanceActivity;
import com.waylens.hachi.ui.clips.player.RawDataLoader;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.clips.share.ShareActivity;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.ui.settings.myvideo.ExportedVideoActivity;
import com.waylens.hachi.ui.transitions.MorphTransform;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.StatusBarHelper;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class PreviewActivity extends ClipPlayActivity {
    public static String TAG = PreviewActivity.class.getSimpleName();
    public static final String EXTRA_PLAYLIST_ID = "playListId";
    public static final String EXTRA_CLIP = "clip";

    private int mPlaylistId = 0;

    private ArrayList<Long> mTimeList = null;

    private String mVin;

    private int raceType = -1;

    private RadioButton btnSd, btnHd, btnFullHd;
    private View mUnselectMaskWithOverlay, mSelectorWithOverlay;
    private View mUnselectMaskWithoutOverlay, mSelectorWithoutOverlay;
    private TextView mExportTip;


    public static void launch(Activity activity, int playlistId, View transitionView) {
        Intent intent = new Intent(activity, PreviewActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);

        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation
            (activity, transitionView, activity.getString(R.string.clip_cover));
        activity.startActivity(intent);
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
//        getRaceTimeInfo();
    }

    public void getRaceTimeInfo() {
        getRaceTimeInfoRx()
            .subscribeOn(Schedulers.io())
            .subscribe(new SimpleSubscribe<List<Long>>() {
                @Override
                public void onNext(List<Long> timeList) {
                    if (timeList != null && timeList.size() > 0) {
//                        mClipPlayFragment.setRaceTimePoints(timeList);
                        mTimeList = new ArrayList<Long>(6);
                        for (int j = 0; j < timeList.size(); j++) {
                            if (timeList.get(j) > 0) {
                                mTimeList.add(j, timeList.get(j));
                            } else {
                                mTimeList.add(j, (long) -1);
                            }
                        }
                    }
                }
            });

    }

    public Observable<List<Long>> getRaceTimeInfoRx() {
        return Observable.create(new Observable.OnSubscribe<List<Long>>() {
            @Override
            public void call(Subscriber<? super List<Long>> subscriber) {
                List<Long> timeList = getRaceTimeList();
                subscriber.onNext(timeList);
            }
        });
    }

    public List<Long> getRaceTimeList() {
        Logger.t(TAG).d("start loading ");
        // First load raw data into memory
        RawDataLoader mRawDataLoader = new RawDataLoader(mPlaylistId);
        mRawDataLoader.loadRawData();

        ClipSet clipSet = getClipSet();
        ArrayList<Clip> clipList;
        if (clipSet != null) {
            clipList = clipSet.getClipList();
        } else {
            Logger.t(TAG).d("clipset empty!");
            return null;
        }

        String vin = null;
        for (Clip clip : clipList) {
            Logger.t(TAG).d("Vin  = " + clip.getVin());
            if (clip.getVin() != null) {
                vin = clip.getVin();
                mVin = vin;
            }
            Logger.t(TAG).d("clip" + clip.cid.type);
            //Clip retClip = loadClipInfo(clip);
            Clip retClip = clip;
            Logger.t(TAG).d("typeRace:" + retClip.typeRace);
            if ((retClip.typeRace & Clip.TYPE_RACE) > 0) {
                raceType = retClip.typeRace;
                RawDataBlock rawDataBlock = mRawDataLoader.loadRawData(clip, RawDataItem.DATA_TYPE_GPS);
                Logger.t(TAG).d("raw data size:" + rawDataBlock.getItemList().size());
                return RaceTimeParseUtils.parseRaceTime(retClip, rawDataBlock);

            }
        }
        return null;
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
                    case R.id.menu_to_download:
                        showExportDetailDialog();
                        break;
                    case R.id.menu_to_enhance:
                        EnhanceActivity.launch(PreviewActivity.this, mPlaylistEditor.getPlaylistId());
                        finish();
                        break;
                    case R.id.menu_to_modify:
                        if (getClipSet().getClip(0).isRaceClip()) {
                            Snackbar.make(getToolbar(), R.string.cannot_modify_for_race, Snackbar.LENGTH_LONG).show();
                        } else {
                            ClipModifyActivity.launch(PreviewActivity.this, getClipSet().getClip(0));
                        }
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


    private void showExportDetailDialog() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .title(R.string.download)
            .customView(R.layout.dialog_download, true)
            .canceledOnTouchOutside(false)
            .positiveText(R.string.ok)
            .negativeText(R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    int streamIndex = Clip.STREAM_MAIN;
                    if (btnSd.isChecked()) {
                        streamIndex = Clip.STREAM_SUB;
                    }
                    boolean withOverlay = mSelectorWithOverlay.getVisibility() == View.VISIBLE;
                    if (withOverlay) {
                        int qualityIndex = 0;
                        if (btnHd.isChecked()) {
                            qualityIndex = 1;
                        } else if (btnFullHd.isChecked()) {
                            qualityIndex = 2;
                        }
                        TranscodingActivity.launch(PreviewActivity.this, mPlaylistId, getClipSet().getClip(0).streams[0], streamIndex, qualityIndex);
                    } else {
                        ExportedVideoActivity.launch(PreviewActivity.this);
                        BgJobHelper.downloadStream(mPlaylistId, getClipSet().getTotalLengthMs(), getClipSet().getClip(0), getClipSet().getClip(0).streams[0], streamIndex);
                    }
                }
            })
            .show();

        btnSd = (RadioButton) dialog.findViewById(R.id.sd_stream);
        btnHd = (RadioButton) dialog.findViewById(R.id.hd_stream);
        btnFullHd = (RadioButton) dialog.findViewById(R.id.full_hd_stream);
        btnHd.setVisibility(!getClipSet().getClip(0).isClipFullHd() ? View.VISIBLE : View.GONE);
        btnFullHd.setVisibility(getClipSet().getClip(0).isClipFullHd() ? View.VISIBLE : View.GONE);
        mExportTip = (TextView) dialog.findViewById(R.id.download_tip);
        FrameLayout layoutWithOverlay = (FrameLayout) dialog.findViewById(R.id.layout_with_overlay);
        FrameLayout layoutWithoutOverlay = (FrameLayout) dialog.findViewById(R.id.layout_without_overlay);
        mUnselectMaskWithOverlay = dialog.findViewById(R.id.unselect_mask_with_overlay);
        mUnselectMaskWithoutOverlay = dialog.findViewById(R.id.unselect_mask_without_overlay);
        mSelectorWithOverlay = dialog.findViewById(R.id.select_mask_with_overlay);
        mSelectorWithoutOverlay = dialog.findViewById(R.id.select_mask_without_overlay);
        layoutWithOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUnselectMaskWithOverlay.setVisibility(View.GONE);
                mUnselectMaskWithoutOverlay.setVisibility(View.VISIBLE);
                mSelectorWithOverlay.setVisibility(View.VISIBLE);
                mSelectorWithoutOverlay.setVisibility(View.GONE);
                btnHd.setVisibility(View.VISIBLE);
                mExportTip.setText(R.string.tip_with_overlay);
            }
        });
        layoutWithoutOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUnselectMaskWithOverlay.setVisibility(View.VISIBLE);
                mUnselectMaskWithoutOverlay.setVisibility(View.GONE);
                mSelectorWithOverlay.setVisibility(View.GONE);
                mSelectorWithoutOverlay.setVisibility(View.VISIBLE);
                if (getClipSet().getClip(0).isClipFullHd()) {
                    btnHd.setVisibility(View.GONE);
                }
                mExportTip.setText(R.string.tip_without_overlay);
            }
        });
        btnSd.setChecked(true);
    }
}
