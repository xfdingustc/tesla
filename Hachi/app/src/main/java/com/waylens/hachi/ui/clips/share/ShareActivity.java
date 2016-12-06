package com.waylens.hachi.ui.clips.share;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
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

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.export.statejobqueue.CacheUploadMomentJob;
import com.waylens.hachi.bgjob.export.statejobqueue.CacheUploadMomentService;
import com.waylens.hachi.bgjob.export.statejobqueue.PersistentQueue;
import com.waylens.hachi.bgjob.export.statejobqueue.StateJobHolder;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.IHachiApi;
import com.waylens.hachi.rest.body.GeoInfo;
import com.waylens.hachi.rest.response.GeoInfoResponse;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.rest.response.VinQueryResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.reative.SnipeApiRx;
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
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.ui.settings.ShareSettingActivity;
import com.waylens.hachi.ui.settings.myvideo.UploadingMomentActivity;
import com.waylens.hachi.ui.views.AvatarView;
import com.waylens.hachi.utils.VersionHelper;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;
import com.waylens.hachi.view.gauge.GaugeSettingManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class ShareActivity extends ClipPlayActivity {
    private static final String TAG = ShareActivity.class.getSimpleName();
    private static final String EXTRA_PLAYLIST_ID = "playlist_id";
    private static final String EXTRA_AUDIO_ID = "audio_id";

    private static final int REQUEST_CODE_FACEBOOK = 0x100;
    private static final int REQUEST_CODE_YOUTUBE = 0x101;
    private static final int REQUEST_SHARE_SETTING = 0x102;

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


    private String[] mSupportedPrivacy;

    private boolean mIsFacebookShareChecked = false;

    private boolean mIsYoutubeShareChecked = false;

    private boolean mIsLocationChecked = true;

    private boolean mIsVehicleInfoChecked = true;

    private LinkedAccounts mLinkedAccounts;

    private IHachiApi mHachi = HachiService.createHachiApiService();

    private SessionManager mSessionManager = SessionManager.getInstance();

    private boolean mAutoDetected;


    private RawDataBlock mRawDataBlock;

    @BindView(R.id.user_vehicle_info)
    TextView mTvUserVehicleInfo;


    @BindView(R.id.info_separator)
    View mInfoSeparator;

    @BindView(R.id.geo_info)
    TextView mTvGeoInfo;

    @BindView(R.id.race_layout)
    LinearLayout mRaceLayout;

    @BindView(R.id.tv_vehicleInfo)
    TextView mTvVehicleInfo;

    @BindView(R.id.vehicle_desc)
    TextInputEditText mVehicleDesc;


    @BindView(R.id.user_avatar)
    AvatarView mUserAvatar;

    @BindView(R.id.user_name)
    TextView mUserName;

    @BindView(R.id.root_scroll_view)
    ScrollView mRootScrollView;

    @BindView(R.id.moment_title)
    TextInputEditText mEtMomentTitle;

//    @BindView(R.id.moment_description)
//    TextInputEditText mEtMomentDescription;

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

    @OnClick(R.id.info_edit)
    public void onBtnInfoEditClicked() {
        ShareSettingActivity.launch(this, mLocation, mVehicleMaker, mVehicleModel, mVehicleYear, mAutoDetected, REQUEST_SHARE_SETTING);

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
            case REQUEST_SHARE_SETTING:
                if (resultCode == Activity.RESULT_OK) {
                    mIsLocationChecked = data.getBooleanExtra(ShareSettingActivity.LOCATION_CHECKED, true);
                    mIsVehicleInfoChecked = data.getBooleanExtra(ShareSettingActivity.VEHICLE_CHECKED, true);
                    mVehicleMaker = data.getStringExtra(ShareSettingActivity.VEHICLE_MAKER);
                    mVehicleModel = data.getStringExtra(ShareSettingActivity.VEHICLE_MODEL);
                    mVehicleYear = data.getIntExtra(ShareSettingActivity.VEHICLE_YEAR, -1);

                    Logger.t(TAG).d("isLocation:" + mIsLocationChecked + "isVehicle:" + mIsVehicleInfoChecked);
                    if (!mIsLocationChecked) {
                        mLocation = null;
                        mTvGeoInfo.setVisibility(View.GONE);
                        mInfoSeparator.setVisibility(View.GONE);
                    }

                    if (mIsVehicleInfoChecked) {
                        if (mVehicleMaker != null) {
                            mTvUserVehicleInfo.setText(mVehicleMaker + " " + mVehicleModel + " " + mVehicleYear);
                        }
                    } else {
                        mTvVehicleInfo.setVisibility(View.GONE);
                        mInfoSeparator.setVisibility(View.GONE);
                    }
                    if (!TextUtils.isEmpty(mVehicleMaker) && !TextUtils.isEmpty(mLocation)) {
                        mInfoSeparator.setVisibility(View.VISIBLE);
                    } else {
                        mInfoSeparator.setVisibility(View.GONE);
                    }
                    Logger.t(TAG).d("maker:" + mVehicleMaker + mVehicleModel + mVehicleYear);
                }
                break;
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
//        mTimingPoints = (ArrayList<Long>) getIntent().getSerializableExtra("mTimingPoints");
//        mRaceType = getIntent().getIntExtra("raceType", -1);
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
                }
            });


        embedVideoPlayFragment();
        setupSocialPolicy();
        mPlayerContainer.post(new Runnable() {
            @Override
            public void run() {
                setupParallex();
            }
        });


        SessionManager sessionManager = SessionManager.getInstance();

        mUserAvatar.loadAvatar(sessionManager.getAvatarUrl(), sessionManager.getUserName());
        mUserName.setText(sessionManager.getUserName());


        Logger.t(TAG).d("is linked with facebook: " + sessionManager.getIsLinked());

        mUserName.requestFocus();
    }

    private void getClipInfo() {
        if (getClipSet().getCount() == 1) {
            checkForRaceType();
        }

        fetchGeoInfo();
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
                    mAutoDetected = true;
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
//                    for (long time : mTimingPoints) {
//                        Logger.t(TAG).d(time);
//                    }
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
            tags, mSocialPrivacy, mAudioId, gaugeSettings, mIsFacebookShareChecked, mIsYoutubeShareChecked, cache);
        if (mMomentType != null && mMomentType.startsWith("RACING")) {
            Logger.t(TAG).d(mMomentType);
            String vehicleDescription = mVehicleDesc.getEditableText().toString();
            localMoment.momentType = mMomentType;
            localMoment.mVehicleDesc = vehicleDescription;
            localMoment.mTimingPoints = mTimingPoints;
        } else {
            ClipSet clipSet = getClipSet();
            if (clipSet.getCount() > 1) {
                localMoment.momentType = "NORMAL_MULTI";
            } else {
                localMoment.momentType = "NORMAL_SINGLE";
            }
        }
        if (mIsVehicleInfoChecked) {
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
        if (mIsLocationChecked) {
            localMoment.withGeoTag = true;
            localMoment.geoInfo = mGeoInfo;
        } else {
            localMoment.withGeoTag = false;
        }
        ToStringUtils.getString(localMoment);
        //BgJobHelper.uploadMoment(localMoment);
        CacheUploadMomentService.scheduleJob(getApplicationContext());
        CacheUploadMomentJob cacheUploadMomentJob = new CacheUploadMomentJob(localMoment);
        StateJobHolder stateJobHolder = new StateJobHolder(cacheUploadMomentJob.getId(), StateJobHolder.INITIAL_STATE, null, cacheUploadMomentJob);
        PersistentQueue.getPersistentQueue().insert(stateJobHolder);
        CacheUploadMomentService.launch(this);
        //PersistentQueue.create();
        UploadingMomentActivity.launch(this);
        finish();
//
    }


}
