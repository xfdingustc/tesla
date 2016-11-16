package com.waylens.hachi.ui.clips;

/**
 * Created by lshw on 16/10/29.
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.jobqueue.scheduling.Scheduler;
import com.waylens.hachi.snipe.remix.AvrproClipInfo;
import com.waylens.hachi.snipe.remix.AvrproFilter;
import com.waylens.hachi.snipe.remix.AvrproSegmentInfo;
import com.waylens.hachi.snipe.utils.ToStringUtils;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.ClipSetManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.clips.enhance.EnhanceActivity;
import com.waylens.hachi.ui.clips.player.RawDataLoader;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


public class RemixActivity extends BaseActivity{
    private static final String TAG = RemixActivity.class.getSimpleName();
    private static final String EXTRA_CLIP_LIST = "clip_list";
    private static final String EXTRA_REMIX_LENGTH = "remix_length";
    private static final String EXTRA_REMIX_MODE = "remix_mode";
    public static final int PLAYLIST_INDEX = 0x100;

    private int mPlaylistId = 0x100;
    private ArrayList<Clip> mClipArrayList;
    private PlayListEditor mPlaylistEditor;
    private AvrproFilter mAvrproFilter;
    private RawDataLoader mRawDataLoader;
    private ArrayList<AvrproSegmentInfo> mFilterResult;
    private ArrayList<Clip> mOriginClipList;
    private Subscription mRemixSubscription;
    private int mRemixLength = 20;
    private int mRemixMode;

    private long RawDataUnitDuration = 5 * 60 * 1000;

    public static void launch(Activity activity, ArrayList<Clip> clipArrayList, int remixLength, int remixMode) {
        Intent intent = new Intent(activity, RemixActivity.class);
        intent.putExtra(EXTRA_CLIP_LIST, clipArrayList);
        intent.putExtra(EXTRA_REMIX_LENGTH, remixLength);
        intent.putExtra(EXTRA_REMIX_MODE, remixMode);
        activity.startActivity(intent);
    }


    @BindView(R.id.remix_progress_bar)
    ProgressBar mRemixProgressBar;

    @BindView(R.id.btn_cancel_remix)
    ImageButton mBtnCancelRemix;

    @BindView(R.id.tv_remix_status)
    TextView mRemixStatus;


    @OnClick(R.id.btn_cancel_remix)
    public void onBtnCancelClicked() {
        Logger.t(TAG).d("on clicked");
        DialogHelper.showLeaveSmartRemixConfirmDialog(this, new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                if (mRemixSubscription != null && !mRemixSubscription.isUnsubscribed()) {
                    mRemixSubscription.unsubscribe();
                }
                RemixActivity.this.finish();
            }
        });
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mClipArrayList != null && mClipArrayList.size() != 0) {
            final int playlistId = 0x100;
            mPlaylistEditor = new PlayListEditor(mVdbRequestQueue, playlistId);

            mPlaylistEditor.buildRx(mClipArrayList)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<Void>(){
                        @Override
                        public void onCompleted() {
                            mRemixProgressBar.setProgress(15);
                            doSmartRemix();
                        }

                        @Override
                        public void onError(Throwable e) {
                            mRemixProgressBar.setProgress(0);
                        }

                        @Override
                        public void onNext(Void aVoid) {
                        }
                    });

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    public void onBackPressed() {
        DialogHelper.showLeaveSmartRemixConfirmDialog(this, new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                if(mRemixSubscription != null) {
                    mRemixSubscription.unsubscribe();
                }
                RemixActivity.this.finish();
            }
        });

    }

    private void doSmartRemix() {
        if (mPlaylistId == -1) {
            return;
        }
        if (mAvrproFilter == null) {
            mAvrproFilter = new AvrproFilter(mRemixMode, this.getExternalCacheDir(), mRemixLength * 1000);
        }
        mRemixSubscription = Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                mRawDataLoader = new RawDataLoader(mPlaylistId, mVdbRequestQueue);
                int ret = mAvrproFilter.init();
                if (ret == 0) {
                    Logger.t(TAG).d("init successfully!");
                } else {
                    Logger.t(TAG).d("init failed!");
                }
                mRemixProgressBar.setProgress(20);
                subscriber.onNext(ret);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .flatMap(new Func1<Integer, Observable<Clip>>() {
                    @Override
                    public Observable<Clip> call(Integer integer) {
                        List<Clip> clips = mRawDataLoader.getClipSet().getClipList();
                        mOriginClipList = (ArrayList<Clip>) clips;
                        for(Clip clip : clips) {
                            Logger.t(TAG).d("clip info:" + clip.getDurationMs());
                        }
                        return Observable.from(clips);
                    }
                })
                .map(new Func1<Clip, Boolean>() {
                    @Override
                    public Boolean call(Clip clip) {
                        Logger.t(TAG).d("clip info:" + clip.getDurationMs());
                        boolean isDataParsed = false;
                        long startTime = clip.getStartTimeMs();
                        long endTime = clip.getStartTimeMs() + clip.getDurationMs();
                        Logger.t(TAG).d("start time:" + startTime + "endTime:" + endTime);
                        for (long start = startTime; start < endTime; start += RawDataUnitDuration) {
                            long duration = Math.min(RawDataUnitDuration, endTime - start);
                            RawDataLoader.RawDataBufAll rawData = mRawDataLoader.loadRawDataBuf(clip, start, (int)duration);
                            Logger.t(TAG).d("start time:" + start);
                            Logger.t(TAG).d("high:" + (int) (start >> 32) + "low:" + (int) (start & 0xffffffff));
                            AvrproClipInfo clipInfo = new AvrproClipInfo(clip.cid.extra, clip.cid.subType, clip.cid.type, (int) (start & 0xffffffff), (int) (start >> 32), (int)duration);
                            isDataParsed = mAvrproFilter.native_avrpro_smart_filter_is_data_parsed(0, clipInfo, (int)(start - startTime), (int)duration);
                            Logger.t(TAG).d("is data parsed:" + isDataParsed);
                            if (!isDataParsed) {
                                Logger.t(TAG).d("got raw data");
                                if (rawData.gpsDataBuf != null) {
                                    int ret = mAvrproFilter.native_avrpro_smart_filter_feed_data(0, rawData.gpsDataBuf, rawData.gpsDataBuf.length, AvrproFilter.DEVICE_ANDROID);
                                    Logger.t(TAG).d("reed ret = " + ret);
                                }
                                if (rawData.iioDataBuf != null) {
                                    int ret = mAvrproFilter.native_avrpro_smart_filter_feed_data(0, rawData.iioDataBuf, rawData.iioDataBuf.length, AvrproFilter.DEVICE_ANDROID);
                                    Logger.t(TAG).d("reed ret = " + ret);
                                }
                                if (rawData.obdDataBuf != null) {
                                    int ret = mAvrproFilter.native_avrpro_smart_filter_feed_data(0, rawData.obdDataBuf, rawData.obdDataBuf.length, AvrproFilter.DEVICE_ANDROID);
                                    Logger.t(TAG).d("reed ret = " + ret);
                                }
                            }
                        }
                        return isDataParsed;
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Boolean>() {
            @Override
            public void onCompleted() {
                Logger.t(TAG).d("got raw data");
                mFilterResult = new ArrayList<>();
                AvrproSegmentInfo segmentInfo = mAvrproFilter.native_avrpro_smart_filter_read_results(0, true);
                if (segmentInfo != null) {
                    mFilterResult.add(segmentInfo);
                }
                Logger.t(TAG).d("read results:" + ToStringUtils.getString(segmentInfo));
                while( (segmentInfo = mAvrproFilter.native_avrpro_smart_filter_read_results(0, false)) != null) {
                    mFilterResult.add(segmentInfo);
                    Logger.t(TAG).d(ToStringUtils.getString(segmentInfo));
                }
                mAvrproFilter.native_avrpro_smart_filter_deint(0);
                toEnhance();
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Boolean b) {
                int progress = mRemixProgressBar.getProgress() + 70/mOriginClipList.size();
                mRemixProgressBar.setProgress(progress);
            }
        });

    }

    private void toEnhance() {
        List<Clip> newClipList = new ArrayList<>();
        if (mFilterResult.size() == 0) {
            mRemixStatus.setText(getString(R.string.remix_failed));
            mRemixProgressBar.setProgress(0);
            return;
        }
        for (AvrproSegmentInfo segmentInfo : mFilterResult) {
            Clip clip = getClipByClipInfo(segmentInfo.parent_clip);
            if (clip != null) {
                Clip newClip = new Clip(clip);
                Logger.t(TAG).d("parent clip start time: " + ((long)segmentInfo.parent_clip.start_time_hi << 32 + segmentInfo.parent_clip.start_time_lo) );
                newClip.editInfo.selectedStartValue = newClip.getStartTimeMs() + segmentInfo.inclip_offset_ms;
                newClip.editInfo.selectedEndValue = newClip.editInfo.selectedStartValue + segmentInfo.duration_ms;
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
                    mRemixProgressBar.setProgress(100);
                    Logger.t(TAG).d("play list size:" + ClipSetManager.getManager().getClipSet(mPlaylistEditor.getPlaylistId()).getClipList().size());
                    EnhanceActivity.launch(RemixActivity.this, mPlaylistId);
                    RemixActivity.this.finish();
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Void aVoid) {

            }
        });
    }

    private Clip getClipByClipInfo(AvrproClipInfo clipInfo) {
        for (Clip clip : mOriginClipList) {
            if (clip.cid.subType == clipInfo.id) {
                return clip;
            }
        }
        return null;
    }

    @Override
    protected void init() {
        super.init();
        initViews();
        Intent intent = getIntent();
        mClipArrayList = (ArrayList<Clip>) intent.getSerializableExtra(EXTRA_CLIP_LIST);
        mRemixLength = intent.getIntExtra(EXTRA_REMIX_LENGTH, 20);
        mRemixMode = intent.getIntExtra(EXTRA_REMIX_MODE, AvrproFilter.SMART_RANDOMCUTTING);
    }

    private void initViews() {
        setContentView(R.layout.activity_remix);
        mRemixProgressBar.setMax(100);
    }

    public ClipSet getClipSet() {
        if (mPlaylistEditor != null) {
            return ClipSetManager.getManager().getClipSet(mPlaylistEditor.getPlaylistId());
        }
        return null;
    }
}
