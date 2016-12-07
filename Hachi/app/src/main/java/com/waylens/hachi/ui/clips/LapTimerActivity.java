package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.eventbus.events.ClipSetChangeEvent;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.eventbus.events.GaugeEvent;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.reative.SnipeApiRx;
import com.waylens.hachi.snipe.remix.AvrproLapData;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipDownloadInfo;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.ClipSetManager;
import com.waylens.hachi.snipe.vdb.ClipSetPos;
import com.waylens.hachi.ui.adapters.GaugeListAdapter;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.clips.editor.clipseditview.ClipsEditView;
import com.waylens.hachi.ui.clips.music.MusicListSelectActivity;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.clips.share.ShareActivity;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.ui.entities.MusicItem;
import com.waylens.hachi.ui.settings.myvideo.ExportedVideoActivity;
import com.waylens.hachi.utils.TapTargetHelper;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;
import com.waylens.hachi.view.gauge.GaugeInfoItem;
import com.waylens.hachi.view.gauge.GaugeSettingManager;

import net.steamcrafted.loadtoast.LoadToast;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;
import it.sephiroth.android.library.tooltip.Tooltip;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by laina on 16/12/7.
 */

public class LapTimerActivity extends ClipPlayActivity {
    private static final String TAG = LapTimerActivity.class.getSimpleName();

    private static final String EXTRA_CLIP_LIST = "clip_list";
    private static final String EXTRA_PLAYLIST_ID = "playlist_id";
    private static final String EXTRA_CLIP_TYPE = "clip_type";

    private static final int CLIP_TYPE_ORDINARY = 0x0000;
    private static final int CLIP_TYPE_LAPTIMER = 0x0001;

    private static final int REQUEST_CODE_ENHANCE = 1000;
    private static final int REQUEST_CODE_ADD_MUSIC = 1001;

    public static final String EXTRA_CLIPS_TO_ENHANCE = "extra.clips.to.enhance";
    public static final String EXTRA_CLIPS_TO_APPEND = "extra.clips.to.append";


    public static final int DEFAULT_AUDIO_ID = -1;

    private static final int ACTION_NONE = -1;
    private static final int ACTION_OVERLAY = 0;
    private static final int ACTION_ADD_VIDEO = 1;

    public static final int PLAYLIST_INDEX = 0x100;

    private MusicItem mMusicItem;
    private int mPlaylistId;
    private GaugeListAdapter mGaugeListAdapter;
    private int mClipType;
    private LoadToast mLoadToast;


    private ClipDownloadInfo.StreamDownloadInfo mDownloadInfo;

    public static void launch(Activity activity, int playlistId, int clipType) {
        Intent intent = new Intent(activity, LapTimerActivity.class);
        intent.putExtra(EXTRA_CLIP_TYPE, clipType);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        activity.startActivity(intent);
    }

    public static void launch(Activity activity, int playlistId) {
        Intent intent = new Intent(activity, LapTimerActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        activity.startActivity(intent);
    }

    private RadioButton btnSd, btnHd, btnFullHd;
    private View mUnselectMaskWithOverlay, mSelectorWithOverlay;
    private View mUnselectMaskWithoutOverlay, mSelectorWithoutOverlay;
    private TextView mExportTip;


    @BindView(R.id.gauge_list_view)
    RecyclerView mGaugeListView;

    @BindView(R.id.clips_edit_view)
    ClipsEditView mClipsEditView;

    @BindView(R.id.enhance_gauge)
    ViewGroup enhanceGauge;

    @BindView(R.id.enhance_action_bar)
    View mEnhanceActionBar;

    @BindView(R.id.btn_gauge)
    View btnGauge;

    @BindView(R.id.btn_music)
    View btnMusic;

    @BindString(R.string.export_clips)
    String strExportClips;

    @BindString(R.string.export_clips_tip)
    String strExportClipsTip;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventClipSetChanged(ClipSetChangeEvent event) {
        Logger.t(TAG).d("on Clip Set chang event clip count: " + getClipSet().getCount());
        setupToolbar();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mEventBus.register(mPlaylistEditor);
        mEventBus.register(mClipsEditView);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mEventBus.unregister(mPlaylistEditor);
        mEventBus.unregister(mClipsEditView);
    }


    @Override
    public void onBackPressed() {
        DialogHelper.showLeaveEnhanceConfirmDialog(this, new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                LapTimerActivity.super.onBackPressed();
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_ENHANCE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ArrayList<Clip> clips = data.getParcelableArrayListExtra(EXTRA_CLIPS_TO_APPEND);
                    Logger.t(TAG).d("append clips: " + clips.size());
                    if (clips.size() > 0) {
                        mPlaylistEditor.addRx(clips)
                                .subscribe(new SimpleSubscribe<Void>() {
                                    @Override
                                    public void onNext(Void aVoid) {

                                    }
                                });
                    }
                    if (getClipSet().getCount() > 0) {
                        btnGauge.setEnabled(true);
                        btnMusic.setEnabled(true);
                        configureActionUI(ACTION_NONE, false);
                    }

                }
                break;
            case REQUEST_CODE_ADD_MUSIC:
                Logger.t(TAG).d("Resultcode: " + resultCode + " data: " + data);
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mMusicItem = MusicItem.fromBundle(data.getBundleExtra("music.item"));
//                    updateMusicUI();
                    btnMusic.setSelected(true);
                    mClipPlayFragment.setAudioUrl(mMusicItem.localPath);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    protected void init() {
        super.init();
        initViews();
        Intent intent = getIntent();
        mPlaylistId = intent.getIntExtra(EXTRA_PLAYLIST_ID, -1);
        mClipType = intent.getIntExtra(EXTRA_CLIP_TYPE, CLIP_TYPE_ORDINARY);
        mPlaylistEditor = new PlayListEditor(mVdbRequestQueue, mPlaylistId);
        mPlaylistEditor.reconstruct();
        if (mClipType == CLIP_TYPE_LAPTIMER) {
            initLaptimerPlaylist();
        }
        embedVideoPlayFragment();
        configEnhanceView();
    }

    private void initViews() {
        setContentView(R.layout.activity_lap_timer);
        setupToolbar();
        mClipsEditView.setVisibility(View.VISIBLE);
        showTagTagetView();
    }

    private void showTagTagetView() {
        if (TapTargetHelper.shouldShowExportTapTarget()) {
            Tooltip.make(this, new Tooltip.Builder()
                    .anchor(getToolbar().findViewById(R.id.menu_to_download), Tooltip.Gravity.BOTTOM)
                    .closePolicy(Tooltip.ClosePolicy.TOUCH_ANYWHERE_CONSUME, -1)
                    .text(strExportClipsTip)
                    .withArrow(true)
                    .withOverlay(true)
                    .maxWidth(getResources().getDisplayMetrics().widthPixels / 2)
                    .withStyleId(R.style.ToolTipLayoutDefaultStyle_Custom1)
                    .floatingAnimation(Tooltip.AnimationBuilder.DEFAULT)
                    .withCallback(new Tooltip.Callback() {
                        @Override
                        public void onTooltipClose(Tooltip.TooltipView tooltipView, boolean b, boolean b1) {

                        }

                        @Override
                        public void onTooltipFailed(Tooltip.TooltipView tooltipView) {

                        }

                        @Override
                        public void onTooltipShown(Tooltip.TooltipView tooltipView) {
                            TapTargetHelper.onShowExportTargetTaped();
                        }

                        @Override
                        public void onTooltipHidden(Tooltip.TooltipView tooltipView) {

                        }
                    })).show();
        }
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.enhance);
        getToolbar().getMenu().clear();
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        getToolbar().inflateMenu(R.menu.menu_enhance);
        if (getClipSet() != null && getClipSet().getCount() == 0) {
            getToolbar().getMenu().removeItem(R.id.menu_to_share);
            getToolbar().getMenu().removeItem(R.id.menu_to_download);
        }
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_to_share:
                        toShare();
                        break;
                    case R.id.menu_to_download:
                        getClipSetDownloadInfo();
                        break;
                }
                return true;
            }

        });
    }
    private void initLaptimerPlaylist() {
        List<AvrproLapData> lapDataList = new ArrayList<>();
        List<Clip> newClipList = new ArrayList<>();
        for (AvrproLapData data : lapDataList) {
            Clip clip = getClipSet().getClip(0);
            if (clip != null) {
                Clip newClip = new Clip(clip);
                newClip.editInfo.selectedStartValue = newClip.getStartTimeMs() + data.inclip_start_offset_ms;
                newClip.editInfo.selectedEndValue = newClip.editInfo.selectedStartValue + data.lap_time_ms;
                newClipList.add(newClip);
                Logger.t(TAG).d("clip start value:" + newClip.getStartTimeMs());
                Logger.t(TAG).d("clip end value:" + (newClip.getStartTimeMs() + newClip.getDurationMs()));
                Logger.t(TAG).d("edit start value:" + newClip.editInfo.selectedStartValue);
                Logger.t(TAG).d("edit end value:" + newClip.editInfo.selectedEndValue);
            }
        }
        mPlaylistEditor.buildRx(newClipList)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        Logger.t(TAG).d("play list size:" + ClipSetManager.getManager().getClipSet(mPlaylistEditor.getPlaylistId()).getClipList().size());
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Void aVoid) {

                    }
                });

    }

    private void configLaptimerView() {
        mClipsEditView.setEditMode(ClipsEditView.EDIT_MODE_LAPTIMER);
        btnGauge.setVisibility(View.GONE);
    }

    private void getClipSetDownloadInfo() {
        if (mLoadToast != null) {
            return;
        }

        mLoadToast = new LoadToast(this);
        mLoadToast.setText(getString(R.string.loading));
        mLoadToast.show();

        Clip.ID cid = new Clip.ID(PLAYLIST_INDEX, 0, null);
        SnipeApiRx.getClipDownloadInfoRx(cid, 0, getClipSet().getTotalLengthMs())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleSubscribe<ClipDownloadInfo>() {
                    @Override
                    public void onNext(ClipDownloadInfo clipDownloadInfo) {
                        mLoadToast.success();
                        mLoadToast = null;
                        showExportDetailDialog(clipDownloadInfo);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mLoadToast.error();
                        mLoadToast = null;
                        super.onError(e);
                    }
                });

    }

    private void showExportDetailDialog(final ClipDownloadInfo clipDownloadInfo) {
        MaterialDialog dialog = new MaterialDialog.Builder(LapTimerActivity.this)
                .title(R.string.download)
                .customView(R.layout.dialog_download, true)
                .canceledOnTouchOutside(false)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        mDownloadInfo = clipDownloadInfo.main;
                        if (btnSd.isChecked()) {
                            mDownloadInfo = clipDownloadInfo.sub;
                        }
                        boolean withOverlay = mSelectorWithOverlay.getVisibility() == View.VISIBLE;
                        if (withOverlay) {
                            int qualityIndex = 0;
                            if (btnHd.isChecked()) {
                                qualityIndex = 1;
                            } else if (btnFullHd.isChecked()) {
                                qualityIndex = 2;
                            }
                            TranscodingActivity.launch(LapTimerActivity.this, mPlaylistId, getClipSet().getClip(0).streams[0], mDownloadInfo, qualityIndex);
                        } else {

                            ExportedVideoActivity.launch(LapTimerActivity.this);
                            BgJobHelper.downloadStream(getClipSet().getClip(0), getClipSet().getClip(0).streams[0], mDownloadInfo, withOverlay);
                        }
                    }
                })
                .show();

        btnSd = (RadioButton) dialog.findViewById(R.id.sd_stream);
        btnHd = (RadioButton) dialog.findViewById(R.id.hd_stream);
        btnFullHd = (RadioButton) dialog.findViewById(R.id.full_hd_stream);
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
                btnHd.setVisibility(View.GONE);
                mExportTip.setText(R.string.tip_without_overlay);
            }
        });
        btnSd.setChecked(true);
    }


    private void toShare() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            AuthorizeActivity.launch(LapTimerActivity.this);
            return;
        } else if (!SessionManager.checkUserVerified(LapTimerActivity.this)) {
            return;
        } else if (getClipSet().getCount() == 0) {
            MaterialDialog dialog = new MaterialDialog.Builder(LapTimerActivity.this)
                    .content(R.string.no_clip_selected)
                    .positiveText(R.string.ok)
                    .show();
        } else {
            ShareActivity.launch(LapTimerActivity.this, mPlaylistEditor.getPlaylistId(), getAudioID());
//            finish();
        }
    }


    private void configureActionUI(int child, boolean isShow) {
        if (isShow && (child != ACTION_NONE)) {
            enhanceGauge.setVisibility(View.VISIBLE);
            mClipsEditView.setVisibility(View.GONE);
        } else {
            enhanceGauge.setVisibility(View.GONE);
            mClipsEditView.setVisibility(View.VISIBLE);
        }
    }


    private void configEnhanceView() {
//        mClipsEditView.setClipIndex(mPlaylistEditor.getPlaylistId());
        if (getClipSet() == null || getClipSet().getClipList() == null) {
            return;
        }
        Logger.t(TAG).d("List size :" + getClipSet().getClipList().size());
        mClipsEditView.setPlayListEditor(mPlaylistEditor);
        mClipsEditView.setOnClipEditListener(new ClipsEditView.OnClipEditListener() {
            @Override
            public void onAddClipClicked() {
                btnMusic.setSelected(false);
                btnGauge.setSelected(false);

                ClipChooserActivity.launch(LapTimerActivity.this, REQUEST_CODE_ENHANCE);
            }

            @Override
            public void onClipSelected(int position, Clip clip) {
                getToolbar().setTitle(R.string.trim);
                mEnhanceActionBar.setVisibility(View.INVISIBLE);
                ClipSetPos clipSetPos = new ClipSetPos(position, clip.editInfo.selectedStartValue);
                mEventBus.post(new ClipSetPosChangeEvent(clipSetPos, TAG));
            }

            @Override
            public void onClipMoved(int fromPosition, final int toPosition, final Clip clip) {

                int selectedPosition = mClipsEditView.getSelectedPosition();
                ClipSetPos clipSetPos = mClipPlayFragment.getClipSetPos();
                if (selectedPosition == -1) {
                    mClipPlayFragment.showClipPosThumbnail(clip, clip.editInfo.selectedStartValue);
                } else if (selectedPosition != clipSetPos.getClipIndex()) {
                    ClipSetPos newClipSetPos = new ClipSetPos(selectedPosition, clip.editInfo.selectedStartValue);
                    mClipPlayFragment.setClipSetPos(newClipSetPos, false);
                }

            }

            @Override
            public void onClipsAppended(List<Clip> clips, int clipCount) {
                if (clips == null) {
                    return;
                }

                if (clipCount > 0) {
                    btnGauge.setEnabled(true);
                    btnMusic.setEnabled(true);
                    configureActionUI(ACTION_NONE, false);
                }
            }

            @Override
            public void onClipRemoved(int clipCount) {

                if (clipCount == 0) {
                    btnGauge.setEnabled(false);
                    btnMusic.setEnabled(false);
                    configureActionUI(ACTION_ADD_VIDEO, true);
                }
            }

            @Override
            public void onExitEditing() {
                getToolbar().setTitle(R.string.enhance);
                mEnhanceActionBar.setVisibility(View.VISIBLE);
            }


            @Override
            public void onStopTrimming(Clip clip) {
                int selectedPosition = mClipsEditView.getSelectedPosition();
                if (selectedPosition == ClipsEditView.POSITION_UNKNOWN) {
                    return;
                }
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mGaugeListView.setLayoutManager(layoutManager);
        mGaugeListAdapter = new GaugeListAdapter(new GaugeListAdapter.OnGaugeItemChangedListener() {
            @Override
            public void onGaugeItemChanged(GaugeInfoItem item) {
                mEventBus.post(new GaugeEvent(GaugeEvent.EVENT_WHAT_UPDATE_SETTING, item));
                GaugeSettingManager.getManager().saveSetting(item);
            }
        });
        mGaugeListView.setAdapter(mGaugeListAdapter);
        configureActionUI(ACTION_NONE, false);
    }


    public ClipSet getClipSet() {
        if (mPlaylistEditor != null) {
            return ClipSetManager.getManager().getClipSet(mPlaylistEditor.getPlaylistId());
        }
        return null;
    }

    private int getAudioID() {
        if (mMusicItem != null) {
            return mMusicItem.id;
        } else {
            return -1;
        }
    }
}
