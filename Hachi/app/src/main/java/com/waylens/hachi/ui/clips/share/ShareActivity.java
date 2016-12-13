package com.waylens.hachi.ui.clips.share;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.Toolbar;
import android.transition.ChangeBounds;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.bgjob.export.statejobqueue.CacheUploadMomentJob;
import com.waylens.hachi.bgjob.export.statejobqueue.CacheUploadMomentService;
import com.waylens.hachi.bgjob.export.statejobqueue.PersistentQueue;
import com.waylens.hachi.bgjob.export.statejobqueue.StateJobHolder;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.IHachiApi;
import com.waylens.hachi.rest.body.GeoInfo;
import com.waylens.hachi.rest.body.LapInfo;
import com.waylens.hachi.rest.response.GeoInfoResponse;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.rest.response.VinQueryResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.reative.SnipeApiRx;
import com.waylens.hachi.snipe.remix.AvrproClipInfo;
import com.waylens.hachi.snipe.remix.AvrproLapData;
import com.waylens.hachi.snipe.remix.AvrproLapTimerResult;
import com.waylens.hachi.snipe.utils.RaceTimeParseUtils;
import com.waylens.hachi.snipe.utils.ToStringUtils;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.rawdata.GpsData;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;
import com.waylens.hachi.ui.adapters.IconSpinnerAdapter;
import com.waylens.hachi.ui.authorization.FacebookAuthorizeActivity;
import com.waylens.hachi.ui.authorization.GoogleAuthorizeActivity;
import com.waylens.hachi.ui.clips.ClipPlayActivity;
import com.waylens.hachi.ui.clips.player.ClipPlayFragment;
import com.waylens.hachi.ui.clips.player.PlaylistUrlProvider;
import com.waylens.hachi.ui.clips.player.UrlProvider;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.ui.settings.VehiclePickActivity;
import com.waylens.hachi.ui.settings.myvideo.UploadingMomentActivity;
import com.waylens.hachi.ui.views.AvatarView;
import com.waylens.hachi.utils.LocalMomentDownloadHelper;
import com.waylens.hachi.utils.VersionHelper;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;
import com.waylens.hachi.view.gauge.GaugeSettingManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.OnClick;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.waylens.hachi.utils.LocalMomentDownloadHelper.DOWNLOAD_STATUS_GET_VIDEO_COVER;
import static com.waylens.hachi.utils.LocalMomentDownloadHelper.DOWNLOAD_STATUS_GET_VIDEO_URL;
import static com.waylens.hachi.utils.LocalMomentDownloadHelper.DOWNLOAD_STATUS_STORE_VIDEO_COVER;
import static com.waylens.hachi.utils.LocalMomentDownloadHelper.DOWNLOAD_STATUS_UPLOAD_UPLOAD_PROGRESS;


/**
 * Created by Xiaofei on 2016/6/16.
 */
public class ShareActivity extends ClipPlayActivity {
    private static final String TAG = ShareActivity.class.getSimpleName();
    private static final String EXTRA_PLAYLIST_ID = "playlist_id";
    private static final String EXTRA_AUDIO_ID = "audio_id";
    private static final String EXTRA_LAP_RESULT = "lap_result";

    private static final int REQUEST_CODE_FACEBOOK = 0x100;
    private static final int REQUEST_CODE_YOUTUBE = 0x101;
    private static final int REQUEST_SHARE_SETTING = 0x102;
    private static final int REQUEST_PICKCAR = 0x103;

    private String mSocialPrivacy;

    private int mPlayListId;

    private String mVin;

    private int mAudioId;

    private int mRaceType;

    public String mVehicleMaker = null;

    public String mVehicleModel = null;

    public int mVehicleYear;

    private String mLocation;

    private GeoInfo mGeoInfo = new GeoInfo();

    public ArrayList<Long> mTimingPoints = null;

    public String mMomentType = null;

    private AvrproLapTimerResult mLapTimerResult;

    private String[] mSupportedPrivacy;

    private boolean mIsFacebookShareChecked = false;

    private boolean mIsYoutubeShareChecked = false;

    private LinkedAccounts mLinkedAccounts;

    private IHachiApi mHachi = HachiService.createHachiApiService();

    private SessionManager mSessionManager = SessionManager.getInstance();

    private RawDataBlock mRawDataBlock;

    private int mStreamId = 0;

    private MaterialDialog mDownloadDialog;


    @BindView(R.id.race_layout)
    LinearLayout mRaceLayout;

    @BindView(R.id.tv_vehicleInfo)
    TextView mTvVehicleInfo;

    @BindView(R.id.vehicle_desc)
    TextInputEditText mVehicleDesc;

    @BindView(R.id.user_avatar)
    AvatarView mUserAvatar;

    @BindView(R.id.user_name)
    TextView tvUserName;

    @BindView(R.id.root_scroll_view)
    ScrollView mRootScrollView;

    @BindView(R.id.moment_title)
    TextInputEditText mEtMomentTitle;

//    @BindView(R.id.moment_description)
//    TextInputEditText mEtMomentDescription;

    @BindView(R.id.user_email)
    TextView tvUserEmail;

    @BindArray(R.array.social_privacy_text)
    CharSequence[] mPrivacyText;

    @BindView(R.id.spinner_social_privacy)
    Spinner mPrivacySpinner;

    @BindView(R.id.other_social)
    TextView mOtherSocial;

    @BindView(R.id.btn_facebook)
    ImageView mBtnFaceBook;

    @BindView(R.id.btn_youtube)
    ImageView mBtnYoutube;

    @BindView(R.id.tv_place)
    TextView tvPlaceInfo;

    @BindView(R.id.switch_show_place)
    Switch switchShowPlace;

    @BindView(R.id.tv_vehicle_info)
    TextView tvVehicleInfo;

    @BindView(R.id.switch_upload_vehicle)
    Switch switchUploadVehicle;

    @BindView(R.id.spinner_upload_resolution)
    Spinner spUploadResolution;

    @OnClick(R.id.tv_vehicle_title)
    public void onVehicleTitleClicked() {
        VehiclePickActivity.launch(this, REQUEST_PICKCAR);
    }

    @OnClick(R.id.btn_facebook)
    public void onBtnFackBookChecked() {
        if (mSessionManager.getFacebookName() == null) {
            FacebookAuthorizeActivity.launch(this, REQUEST_CODE_FACEBOOK);
        } else {
            mIsFacebookShareChecked = !mIsFacebookShareChecked;
            if (mIsFacebookShareChecked) {
                mBtnFaceBook.setImageResource(R.drawable.ic_facebook_enable);
            } else {
                mBtnFaceBook.setImageResource(R.drawable.ic_facebook_disable);
            }
        }
    }

    @OnClick(R.id.btn_youtube)
    public void onBtnYoutubeChecked() {
        if (!mSessionManager.isYoutubeLinked()) {
            GoogleAuthorizeActivity.launch(this, REQUEST_CODE_YOUTUBE);
        } else {
            mIsYoutubeShareChecked = !mIsYoutubeShareChecked;
            if (mIsYoutubeShareChecked) {
                mBtnYoutube.setImageResource(R.drawable.ic_youtube_enable);
            } else {
                mBtnYoutube.setImageResource(R.drawable.ic_facebook_disable);
            }
        }
    }

    public static void launch(Activity activity, int playListId, int audioId) {
        Intent intent = new Intent(activity, ShareActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playListId);
        intent.putExtra(EXTRA_AUDIO_ID, audioId);
        activity.startActivity(intent);
    }

    public static void launch(Activity activity, int playListId, int audioId, AvrproLapTimerResult lapTimerResult) {
        Intent intent = new Intent(activity, ShareActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playListId);
        intent.putExtra(EXTRA_AUDIO_ID, audioId);
        intent.putExtra(EXTRA_LAP_RESULT, lapTimerResult);
        activity.startActivity(intent);
    }


    public static void launch(Activity activity, int playListId, int audioId, String vin, ArrayList<Long> timingPoints, int raceType) {
        Intent intent = new Intent(activity, ShareActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playListId);
        intent.putExtra(EXTRA_AUDIO_ID, audioId);
        if (vin != null) {
            intent.putExtra("vin", vin);
        }
        if (timingPoints != null && timingPoints.size() > 0) {
            intent.putExtra("mTimingPoints", timingPoints);
        }
        intent.putExtra("raceType", raceType);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PICKCAR:
                if (resultCode == Activity.RESULT_OK) {
                    mVehicleMaker = data.getStringExtra(VehiclePickActivity.VEHICLE_MAKER);
                    mVehicleModel = data.getStringExtra(VehiclePickActivity.VEHICLE_MODEL);
                    mVehicleYear = data.getIntExtra(VehiclePickActivity.VEHICLE_YEAR, 0);
                    tvVehicleInfo.setText(mVehicleMaker + " " + mVehicleModel + " " + mVehicleYear);
                    tvVehicleInfo.setVisibility(View.VISIBLE);
                    switchUploadVehicle.setChecked(true);
                }
            default:
                break;
        }
    }


    @Override
    protected void init() {
        super.init();
        mPlayListId = getIntent().getIntExtra(EXTRA_PLAYLIST_ID, -1);
        mAudioId = getIntent().getIntExtra(EXTRA_AUDIO_ID, -1);
        mVin = getIntent().getStringExtra("vin");
        mLapTimerResult = (AvrproLapTimerResult) getIntent().getSerializableExtra(EXTRA_LAP_RESULT);
        initViews();
    }


    private void initViews() {
        setContentView(R.layout.activity_share);
        setupToolbar();
        mPlaylistEditor = new PlayListEditor(mVdbRequestQueue, mPlayListId);
        mPlaylistEditor.reconstruct();


        SnipeApiRx.getRawDataBlockRx(getClipSet().getClip(0), RawDataItem.DATA_TYPE_GPS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<RawDataBlock>() {
                @Override
                public void onNext(RawDataBlock rawDataBlock) {
                    mRawDataBlock = rawDataBlock;
                }

                @Override
                public void onCompleted() {
                    getClipInfo();
                    if (mLapTimerResult != null) {
                        Clip clip = getClipSet().getClip(0);
                        Logger.t(TAG).d("share clip startTimeMs:" + clip.editInfo.selectedStartValue);
                        AvrproClipInfo clipInfo = new AvrproClipInfo(clip.cid.extra, clip.cid.subType, clip.cid.type,
                                (int) (clip.editInfo.selectedStartValue), (int) (clip.editInfo.selectedStartValue >> 32), clip.editInfo.getSelectedLength());
                        mClipPlayFragment.getGaugeView().setLapTimerData(mLapTimerResult, clipInfo);
                        mMomentType = "LAP_TIMER";
                    }
                }
            });

        embedVideoPlayFragment(false);
        setupSocialPolicy();
        mPlayerContainer.post(new Runnable() {
            @Override
            public void run() {
                setupParallex();
            }
        });


        SessionManager sessionManager = SessionManager.getInstance();

        mUserAvatar.loadAvatar(sessionManager.getAvatarUrl(), sessionManager.getUserName());
        tvUserName.setText(sessionManager.getUserName());
        tvUserEmail.setText(sessionManager.getEmail());


        Logger.t(TAG).d("is linked with facebook: " + sessionManager.getIsLinked());

        spUploadResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mStreamId = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                mStreamId = 0;
            }
        });


    }

    private void getClipInfo() {
        if (getClipSet().getCount() == 1) {
            checkForRaceType();
        }
        if (mRawDataBlock != null) {
            fetchGeoInfo();
        }
        getClipVinNumber();
    }

    private void fetchGeoInfo() {
        GpsData gpsData = (GpsData) mRawDataBlock.getRawDataItem(0).data;
        double lat = gpsData.coord.lat;
        double lng = gpsData.coord.lng;
        mGeoInfo.latitude = lat;
        mGeoInfo.longitude = lng;
        mHachi.getGeoInfoRx(lng, lat)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<GeoInfoResponse>() {
                @Override
                public void onNext(GeoInfoResponse geoInfoResponse) {
                    mLocation = geoInfoResponse.getLocationString();
                    mGeoInfo.city = geoInfoResponse.city;
                    mGeoInfo.country = geoInfoResponse.country;
                    mGeoInfo.region = geoInfoResponse.region;
                    tvPlaceInfo.setText(mLocation);
                    tvPlaceInfo.setVisibility(View.VISIBLE);
                    switchShowPlace.setChecked(true);
                }
            });

    }


    private void getClipVinNumber() {
        mPlaylistEditor.doGetPlaylistInfoDetailedRx()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<ClipSet>() {
                @Override
                public void onNext(ClipSet clipSet) {
                    if (clipSet.getClipList().size() > 0) {
                        mVin = clipSet.getClip(0).getVin();
                        String vin = mVin.substring(0, 8) + mVin.substring(9, 11);
                        fetchVehicleInfo(vin);
                    }
                }
            });
    }

    private void fetchVehicleInfo(String vin) {
        mHachi.queryByVinRx(vin)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<VinQueryResponse>() {
                @Override
                public void onNext(VinQueryResponse vinQueryResponse) {
                    mVehicleMaker = vinQueryResponse.makerName;
                    mVehicleModel = vinQueryResponse.modelName;
                    mVehicleYear = vinQueryResponse.year;
                    tvVehicleInfo.setText(mVehicleMaker + " " + mVehicleModel + " " + mVehicleYear);
                    tvVehicleInfo.setVisibility(View.VISIBLE);
                    switchUploadVehicle.setChecked(true);
                    Logger.t(TAG).d("vin query response:" + vinQueryResponse.makerName + vinQueryResponse.modelName + vinQueryResponse.year);
                }
            });

    }


    private void checkForRaceType() {
        SnipeApiRx.getClipInfo(getClipSet().getClip(0))
            .subscribeOn(Schedulers.io())
            .subscribe(new SimpleSubscribe<Clip>() {
                @Override
                public void onNext(Clip clip) {
                    if ((clip.typeRace & Clip.TYPE_RACE) <= 0) {
                        return;
                    }
                    switch (clip.typeRace & Clip.MASK_RACE) {
                        case Clip.TYPE_RACE_AU3T:
                            mMomentType = "RACING_AU3T";
                            break;
                        case Clip.TYPE_RACE_AU6T:
                            mMomentType = "RACING_AU6T";
                            break;
                        case Clip.TYPE_RACE_CD3T:
                            mMomentType = "RACING_CD3T";
                            break;
                        case Clip.TYPE_RACE_CD6T:
                            mMomentType = "RACING_CD6T";
                            break;
                        default:
                            break;
                    }

                    Logger.t(TAG).d("raw data size:" + mRawDataBlock.getItemList().size());
                    List<Long> timeList = RaceTimeParseUtils.parseRaceTime(clip, mRawDataBlock);
                    mTimingPoints = new ArrayList<Long>(6);
                    for (int j = 0; j < timeList.size(); j++) {
                        if (timeList.get(j) > 0) {
                            mTimingPoints.add(j, timeList.get(j));
                        } else {
                            mTimingPoints.add(j, (long) -1);
                        }
                    }
                }
            });
    }


    private void setupSocialPolicy() {
        TypedArray typedArray = getResources().obtainTypedArray(R.array.social_privacy_icon);
        Drawable[] drawables = new Drawable[typedArray.length()];
        for (int i = 0; i < drawables.length; i++) {
            drawables[i] = typedArray.getDrawable(i);
        }
        IconSpinnerAdapter mAdapter = new IconSpinnerAdapter(this, android.R.layout.simple_spinner_item, mPrivacyText, drawables,
            ViewUtils.dp2px(16));
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPrivacySpinner.setAdapter(mAdapter);

        mSupportedPrivacy = getResources().getStringArray(R.array.social_privacy_value);
        mSocialPrivacy = mSupportedPrivacy[0];
        mPrivacySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSocialPrivacy = mSupportedPrivacy[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSocialPrivacy = mSupportedPrivacy[0];
            }
        });
    }

    @Override
    protected void embedVideoPlayFragment(boolean transition) {

        UrlProvider vdtUriProvider = new PlaylistUrlProvider(mPlaylistEditor.getPlaylistId());

        if (mLapTimerResult != null) {
            mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, mPlaylistEditor.getPlaylistId(),
                    vdtUriProvider, ClipPlayFragment.ClipMode.MULTI, ClipPlayFragment.CoverMode.NORMAL, ClipPlayFragment.VIDEO_TYPE_LAPTIMER);
        } else {
            mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, mPlaylistEditor.getPlaylistId(),
                    vdtUriProvider, ClipPlayFragment.ClipMode.MULTI, ClipPlayFragment.CoverMode.NORMAL);
        }

        if (transition) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mClipPlayFragment.setSharedElementEnterTransition(new ChangeBounds());
            }
        }

        getFragmentManager().beginTransaction().replace(R.id.player_fragment_content, mClipPlayFragment).commit();
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.share);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getToolbar().inflateMenu(R.menu.menu_share);
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.share:
                        doShareMoment(true);
                        break;
                }
                return true;
            }
        });
    }


    private void setupParallex() {
        if (VersionHelper.isGreateThanMashmellow()) {
            final int width = mPlayerContainer.getMeasuredWidth();
            final int height = mPlayerContainer.getMeasuredHeight();
            final float originY = mPlayerContainer.getY();


            mRootScrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, final int scrollY, int oldScrollX, int oldScrollY) {
                    mPlayerContainer.setY(originY + scrollY / 2);

                    mPlayerContainer.setClipToOutline(true);
                    mPlayerContainer.setOutlineProvider(new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, Outline outline) {
                            Rect rect = new Rect(0, scrollY / 2, width, height - scrollY / 2);
                            outline.setRect(rect);
                        }
                    });

                }
            });
        }

    }

    private void doShareMoment(boolean cache) {
        String title = mEtMomentTitle.getEditableText().toString();
//        if (TextUtils.isEmpty(name)) {
//            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
//            name = "Created " + format.format(System.currentTimeMillis());
//        }

        String descrption = "";//mEtMomentDescription.getEditableText().toString();
        String[] tags = new String[]{};

        Map<String, String> gaugeSettings = GaugeSettingManager.getManager().getGaugeSettingMap();


        Logger.t(TAG).d("share name: " + title);

        LocalMoment localMoment = new LocalMoment(mPlaylistEditor.getPlaylistId(), title, descrption,
            tags, mSocialPrivacy, mAudioId, gaugeSettings, mIsFacebookShareChecked,
            mIsYoutubeShareChecked, cache, mStreamId);

        if (mMomentType != null && mMomentType.startsWith("RACING")) {
            Logger.t(TAG).d(mMomentType);
            String vehicleDescription = mVehicleDesc.getEditableText().toString();
            localMoment.momentType = mMomentType;
            localMoment.mVehicleDesc = vehicleDescription;
            localMoment.mTimingPoints = mTimingPoints;
        } else if (mMomentType != null && mMomentType.equals("LAP_TIMER")) {
            localMoment.momentType = mMomentType;
            localMoment.gaugeSettings.put("theme", "rifle");
            LapInfo.LapTimer lapTimerHeader = new LapInfo.LapTimer();
            lapTimerHeader.totalLaps = mLapTimerResult.lapsHeader.total_laps;
            lapTimerHeader.bestLapTime = mLapTimerResult.lapsHeader.best_lap_time_ms;
            lapTimerHeader.bestLapSpeed = mLapTimerResult.lapsHeader.top_speed_kph;
            lapTimerHeader.checkPoints = 1000;
            Clip clip = getClipSet().getClip(0);
            ArrayList<LapInfo.LapData> lapDatas = new ArrayList<>();
            for (AvrproLapData lapData : mLapTimerResult.lapList) {
                LapInfo.LapData perLap = new LapInfo.LapData();
                perLap.totalLapTime = lapData.lap_time_ms;
                perLap.checkIntervalMs = lapData.check_interval_ms;
                perLap.startOffsetMs = lapData.inclip_start_offset_ms;
                perLap.deltaMsToBest = new ArrayList<>();
                for(int i = 0; i < lapTimerHeader.bestLapTime / lapData.check_interval_ms; i++) {
                    perLap.deltaMsToBest.add(lapData.delta_ms_to_best[i]);
                }
                lapDatas.add(perLap);
            }
            localMoment.lapInfo = new LapInfo(lapTimerHeader, lapDatas);
        } else {
            ClipSet clipSet = getClipSet();
            if (clipSet.getCount() > 1) {
                localMoment.momentType = "NORMAL_MULTI";
            } else {
                localMoment.momentType = "NORMAL_SINGLE";
            }
        }
        if (switchUploadVehicle.isChecked()) {
            localMoment.withCarInfo = true;
            localMoment.mVehicleMaker = mVehicleMaker;
            localMoment.mVehicleModel = mVehicleModel;
            localMoment.mVehicleYear = mVehicleYear;
            if (mVin != null) {
                localMoment.vin = mVin.substring(0, 8) + mVin.substring(9, 11);
            }
        } else {
            localMoment.withCarInfo = false;
        }
        if (switchShowPlace.isChecked()) {
            localMoment.withGeoTag = true;
            localMoment.geoInfo = mGeoInfo;
        } else {
            localMoment.withGeoTag = false;
        }
        ToStringUtils.getString(localMoment);
        showDownloadDialog(localMoment);
        //BgJobHelper.uploadMoment(localMoment);
/*        CacheUploadMomentService.scheduleJob(getApplicationContext());
        CacheUploadMomentJob cacheUploadMomentJob = new CacheUploadMomentJob(localMoment);
        StateJobHolder stateJobHolder = new StateJobHolder(cacheUploadMomentJob.getId(), StateJobHolder.INITIAL_STATE, null, cacheUploadMomentJob);
        PersistentQueue.getPersistentQueue().insert(stateJobHolder);
        CacheUploadMomentService.launch(this);*/
        //UploadingMomentActivity.launch(this);
        //finish();
//
    }

    private void showDownloadDialog(final LocalMoment localMoment) {
        LocalMomentDownloadHelper.downloadLocalMomentRx(localMoment)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<LocalMomentDownloadHelper.DownloadLocalMomentStatus>() {
                @Override
                public void onCompleted() {
                    if (mDownloadDialog != null && mDownloadDialog.isShowing()) {
                        mDownloadDialog.dismiss();
                    }
                    BgJobHelper.uploadCachedMoment(localMoment);
                    UploadingMomentActivity.launch(ShareActivity.this);
                    finish();

                }

                @Override
                public void onError(Throwable e) {
                    if (mDownloadDialog != null && mDownloadDialog.isShowing()) {
                        mDownloadDialog.dismiss();
                    }

                    Snackbar.make(mRootScrollView, R.string.download_clip_error, Snackbar.LENGTH_LONG).show();
                }

                @Override
                public void onNext(LocalMomentDownloadHelper.DownloadLocalMomentStatus downloadLocalMomentStatus) {
                    switch (downloadLocalMomentStatus.status) {
                        case DOWNLOAD_STATUS_GET_VIDEO_URL:
                            mDownloadDialog.setTitle(R.string.upload_get_url_info);
                            break;
                        case DOWNLOAD_STATUS_GET_VIDEO_COVER:
                            mDownloadDialog.setTitle(R.string.upload_get_video_cover);
                            break;
                        case DOWNLOAD_STATUS_STORE_VIDEO_COVER:
                            mDownloadDialog.setTitle(R.string.upload_store_video_cover);
                            break;
                        case DOWNLOAD_STATUS_UPLOAD_UPLOAD_PROGRESS:
                            mDownloadDialog.setTitle(R.string.cache_start);
                            mDownloadDialog.setProgress(downloadLocalMomentStatus.progress);
                            break;
                    }
                }
            });

        mDownloadDialog = new MaterialDialog.Builder(this)
            .title(R.string.download)
            .progress(false, 100)
            .negativeText(R.string.cancel)
            .canceledOnTouchOutside(false)
            .onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                }
            })
            .show();

    }


}
