package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.ChangeBounds;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.ClipSetChangeEvent;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.reative.SnipeApiRx;
import com.waylens.hachi.snipe.remix.AvrproClipInfo;
import com.waylens.hachi.snipe.remix.AvrproFilter;
import com.waylens.hachi.snipe.remix.AvrproGpsParsedData;
import com.waylens.hachi.snipe.remix.AvrproLapData;
import com.waylens.hachi.snipe.remix.AvrproLapTimerResult;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipDownloadInfo;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.ClipSetManager;
import com.waylens.hachi.snipe.vdb.ClipSetPos;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.clips.editor.clipseditview.ClipsEditView;
import com.waylens.hachi.ui.clips.player.ClipPlayFragment;
import com.waylens.hachi.ui.clips.player.PlaylistUrlProvider;
import com.waylens.hachi.ui.clips.player.RawDataLoader;
import com.waylens.hachi.ui.clips.player.UrlProvider;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.clips.share.ShareActivity;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.ui.entities.MusicItem;
import com.waylens.hachi.ui.settings.myvideo.ExportedVideoActivity;
import com.waylens.hachi.utils.TooltipHelper;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;
import net.steamcrafted.loadtoast.LoadToast;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import it.sephiroth.android.library.tooltip.Tooltip;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by laina on 16/12/7.
 */

public class LapTimerActivity extends ClipPlayActivity implements LapListAdapter.OnLapClickListener{
    private static final String TAG = LapTimerActivity.class.getSimpleName();

    private static final String EXTRA_CLIP_LIST = "clip_list";
    private static final String EXTRA_PLAYLIST_ID = "playlist_id";
    private static final String EXTRA_CLIP_TYPE = "clip_type";

    private static final int REQUEST_CODE_ENHANCE = 1000;
    private static final int REQUEST_CODE_ADD_MUSIC = 1001;

    public static final String EXTRA_CLIPS_TO_APPEND = "extra.clips.to.append";

    public static final int NORMAL_MODE = 0x0001;
    public static final int SHARE_MODE = 0x0002;
    public static final int EXPORT_MODE = 0x0003;
    public static final int DEFAULT_AUDIO_ID = -1;

    private static final int ACTION_NONE = -1;
    private static final int ACTION_OVERLAY = 0;
    private static final int ACTION_ADD_VIDEO = 1;

    public static final int PLAYLIST_INDEX = 0x100;

    private MusicItem mMusicItem;
    private int mPlaylistId;
    private LoadToast mLoadToast;
    private AvrproFilter mAvrproFilter;
    private Clip mOriginClip;
    private AvrproLapTimerResult mLapTimerResult;
    private LapListAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private int mMode;

    private ClipDownloadInfo.StreamDownloadInfo mDownloadInfo;

    public static void launch(Activity activity, int playlistId, int clipType) {
        Intent intent = new Intent(activity, LapTimerActivity.class);
        intent.putExtra(EXTRA_CLIP_TYPE, clipType);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        activity.startActivity(intent);
    }

    public static void launch(Activity activity, int playlistId, View transitionView) {
        Intent intent = new Intent(activity, LapTimerActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation
                (activity, transitionView, activity.getString(R.string.clip_cover));
        activity.startActivity(intent, options.toBundle());
    }

    private RadioButton btnSd, btnHd, btnFullHd;
    private View mUnselectMaskWithOverlay, mSelectorWithOverlay;
    private View mUnselectMaskWithoutOverlay, mSelectorWithoutOverlay;
    private TextView mExportTip;

    ViewGroup enhanceGauge;

    @BindView(R.id.rv_lap_data)
    RecyclerView rvLapData;

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
        //mEventBus.register(mClipsEditView);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mEventBus.unregister(mPlaylistEditor);
        //mEventBus.unregister(mClipsEditView);
    }


    @Override
    public void onBackPressed() {
        DialogHelper.showLeaveEnhanceConfirmDialog(this, new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                LapTimerActivity.super.onBackPressed();
                LapTimerActivity.this.finish();
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
                        configureActionUI(ACTION_NONE, false);
                    }

                }
                break;
            case REQUEST_CODE_ADD_MUSIC:
                Logger.t(TAG).d("Resultcode: " + resultCode + " data: " + data);
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mMusicItem = MusicItem.fromBundle(data.getBundleExtra("music.item"));
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
        mPlaylistEditor = new PlayListEditor(mVdbRequestQueue, mPlaylistId);
        mPlaylistEditor.reconstruct();
        parseLapTimerInfo();
        mMode = NORMAL_MODE;
        mAdapter = new LapListAdapter(this, this);
        rvLapData.setLayoutManager(new LinearLayoutManager(this));
        rvLapData.setAdapter(mAdapter);
        //initLaptimerPlaylist();
        embedVideoPlayFragment();
        //configEnhanceView();
    }

    private void initViews() {
        setContentView(R.layout.activity_lap_timer);
        setupToolbar();
        showTagTagetView();
    }

    private void showTagTagetView() {
        if (TooltipHelper.shouldShowExportTapTarget()) {
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
                            TooltipHelper.onShowExportTargetTaped();
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
        getToolbar().setTitle("Lap Timer");
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

    private void parseLapTimerInfo() {
        Observable.create(new Observable.OnSubscribe<AvrproLapTimerResult>() {
            @Override
            public void call(Subscriber<? super AvrproLapTimerResult> subscriber) {
                ClipSet clipSet = getClipSet();
                mOriginClip = clipSet.getClip(0);
                AvrproClipInfo clipInfo = new AvrproClipInfo(mOriginClip.cid.extra, mOriginClip.cid.subType, mOriginClip.cid.type,
                        (int) (mOriginClip.getStartTimeMs()), (int) (mOriginClip.getStartTimeMs() >> 32), mOriginClip.getDurationMs());
                mAvrproFilter = new AvrproFilter(clipInfo);
                int ret = mAvrproFilter.init();
                if (ret == 0) {
                    Logger.t(TAG).d("init successfully!");
                } else {
                    Logger.t(TAG).d("init failed!");
                }
                if (mOriginClip.lapTimerData == null) {
                    return;
                }
                Clip.LapTimerData lapTimerData = mOriginClip.lapTimerData;
                AvrproGpsParsedData startNode = new AvrproGpsParsedData(lapTimerData.latitude, lapTimerData.longitude,
                        lapTimerData.utcTime, lapTimerData.utcTimeUsec);
                mAvrproFilter.native_avrpro_lap_timer_set_start(ret, startNode);
                byte[] gpsBuf = RawDataLoader.getRawDataBuf(mOriginClip, RawDataItem.DATA_TYPE_GPS, mOriginClip.getStartTimeMs(),
                                                        mOriginClip.getDurationMs());
                mAvrproFilter.native_avrpro_lap_timer_feed_gps_data(0, gpsBuf, gpsBuf.length, AvrproFilter.DEVICE_ANDROID);
                AvrproLapTimerResult result = mAvrproFilter.native_avrpro_lap_timer_read_results(0);
                subscriber.onNext(result);
                mAvrproFilter.native_avrpro_lap_timer_deinit(ret);
            }
        }).subscribeOn(Schedulers.newThread())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Subscriber<AvrproLapTimerResult>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(AvrproLapTimerResult result) {
                Logger.t(TAG).d("total laps = " + result.lapsHeader.total_laps);
                for (AvrproGpsParsedData data : result.gpsList) {
                    data.clip_time_ms = data.clip_time_ms - mOriginClip.getStartTimeMs();
                }
                mAdapter.setLapDataList(Arrays.asList(result.lapList));
                mClipPlayFragment.getGaugeView().setLapTimerData(result, mAvrproFilter.getClipInfo());
                mLapTimerResult = result;
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

    @Override
    protected void embedVideoPlayFragment() {

        UrlProvider vdtUriProvider = new PlaylistUrlProvider(mPlaylistEditor.getPlaylistId());

        mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, mPlaylistEditor.getPlaylistId(),
                vdtUriProvider, ClipPlayFragment.ClipMode.MULTI, ClipPlayFragment.CoverMode.NORMAL, ClipPlayFragment.VIDEO_TYPE_LAPTIMER);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mClipPlayFragment.setSharedElementEnterTransition(new ChangeBounds());
        }
        getFragmentManager().beginTransaction().replace(R.id.player_fragment_content, mClipPlayFragment).commit();
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
//                            TranscodingActivity.launch(LapTimerActivity.this, mPlaylistId, getClipSet().getClip(0).streams[0], mDownloadInfo, qualityIndex);
                        } else {

                            ExportedVideoActivity.launch(LapTimerActivity.this);
//                            BgJobHelper.downloadStream(getClipSet().getClip(0), getClipSet().getClip(0).streams[0], mDownloadInfo, withOverlay);
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

    private void toExport(int lapId) {

    }

    private void toSeek(int lapId) {
        AvrproLapData lapData = Arrays.asList(mLapTimerResult.lapList).get(lapId);
        ClipSetPos newClipSetPos= getClipSet().getClipSetPosByTimeOffset(lapData.inclip_start_offset_ms);
        mClipPlayFragment.setClipSetPos(newClipSetPos, true);
        mEventBus.post(new ClipSetPosChangeEvent(newClipSetPos, ClipPlayFragment.class.getSimpleName()));
        mClipPlayFragment.startPreparingClip(newClipSetPos, false);
    }


    private void configureActionUI(int child, boolean isShow) {
        if (isShow && (child != ACTION_NONE)) {
            enhanceGauge.setVisibility(View.VISIBLE);
        } else {
            enhanceGauge.setVisibility(View.GONE);
        }
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

    @Override
    public void onLapClicked(int lapId) {
        switch (mMode) {
            case NORMAL_MODE:
                toSeek(lapId);
                break;
            case SHARE_MODE:
                toShare();
                break;
            case EXPORT_MODE:
                toExport(lapId);
                break;
            default:
                break;
        }
    }
}
