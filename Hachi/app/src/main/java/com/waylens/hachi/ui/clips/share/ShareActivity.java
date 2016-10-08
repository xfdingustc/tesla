package com.waylens.hachi.ui.clips.share;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.GaugeSettingManager;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.GeoInfo;
import com.waylens.hachi.rest.response.VinQueryResponse;
import com.waylens.hachi.rest.response.GeoInfoResponse;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.adapters.IconSpinnerAdapter;
import com.waylens.hachi.ui.authorization.FacebookAuthorizeActivity;
import com.waylens.hachi.ui.authorization.GoogleAuthorizeActivity;
import com.waylens.hachi.ui.clips.ClipPlayActivity;
import com.waylens.hachi.ui.clips.player.RawDataLoader;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.ui.settings.ShareSettingActivity;
import com.waylens.hachi.ui.settings.myvideo.UploadingMomentActivity;
import com.waylens.hachi.utils.StringUtils;
import com.waylens.hachi.utils.ViewUtils;
import com.xfdingustc.snipe.utils.ToStringUtils;
import com.xfdingustc.snipe.vdb.Clip;
import com.xfdingustc.snipe.vdb.ClipSet;
import com.xfdingustc.snipe.vdb.rawdata.GpsData;
import com.xfdingustc.snipe.vdb.rawdata.RawDataBlock;
import com.xfdingustc.snipe.vdb.rawdata.RawDataItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TimeZone;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Response;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
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

    private int mPlayListId;

    private String mVin;

    private int mAudioId;

    private int mRaceType;

    public String mVehicleMaker = null;

    public String mVehicleModel = null;

    public int mVehicleYear;

    private String mLocation;

    private GeoInfo mGeoInfo = new GeoInfo();

    public ArrayList<Long> timingPoints = null;

    public String momentType = null;

    private MaterialDialog mUploadDialog;

    private String[] mSupportedPrivacy;

    private boolean mIsFacebookShareChecked = false;

    private boolean mIsYoutubeShareChecked = false;

    private boolean mIsLocationChecked = true;

    private boolean mIsVehicleInfoChecked = true;

    private LinkedAccounts mLinkedAccounts;

    private HachiApi mHachi = HachiService.createHachiApiService();

    private SessionManager mSessionManager = SessionManager.getInstance();

    private boolean mAutoDetected;

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

    private String mSocialPrivacy;
    @BindView(R.id.user_avatar)
    CircleImageView mUserAvatar;

    @BindView(R.id.user_name)
    TextView mUserName;

    @BindView(R.id.root_scroll_view)
    ScrollView mRootScrollView;

    @BindView(R.id.moment_title)
    TextInputEditText mEtMomentTitle;

    @BindView(R.id.moment_description)
    TextInputEditText mEtMomentDescription;

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
                mBtnFaceBook.setImageResource(R.drawable.btn_platform_facebook_s);
            } else {
                mBtnFaceBook.setImageResource(R.drawable.btn_platform_facebook_n);
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
                mBtnYoutube.setImageResource(R.drawable.btn_platform_youtube_s);
            } else {
                mBtnYoutube.setImageResource(R.drawable.btn_platform_youtube_n);
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
            intent.putExtra("timingPoints", timingPoints);
        }
        intent.putExtra("raceType", raceType);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVin = getIntent().getStringExtra("vin");
        timingPoints = (ArrayList<Long>) getIntent().getSerializableExtra("timingPoints");
        mRaceType = getIntent().getIntExtra("raceType", -1);
        init();
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
        initViews();
    }


    private void initViews() {
        setContentView(R.layout.activity_share);
        setupToolbar();
        Logger.t(TAG).d("init");
        Logger.t(TAG).d("timezone offset" + TimeZone.getDefault().getRawOffset());
        mPlaylistEditor = new PlayListEditor(mVdbRequestQueue, mPlayListId);
        mPlaylistEditor.reconstruct();
        Observable.create(new Observable.OnSubscribe<Clip>() {
            @Override
            public void call(Subscriber<? super Clip> subscriber) {
                Logger.t(TAG).d("init views");
                ClipSet clipSet = null;
                try {
                    clipSet = mPlaylistEditor.doGetPlaylistInfoDetailed();
                    Clip clip = null;
                    if (mRaceType >= 0 && clipSet.getCount() == 1) {
                        clip = clipSet.getClip(0);
                        clip.raceTimingPoints = timingPoints;
                        clip.typeRace = mRaceType;
                        subscriber.onNext(clip);
                    } else {
                        mVin = clipSet.getClip(0).getVin();
                    }
                } catch (Exception e) {
                    Logger.t(TAG).d(e.getMessage());
                    subscriber.onError(e);
                }

                if (mVin != null) {
                    String vin = mVin.substring(0, 8) + mVin.substring(9, 11);
                    try {
                        Logger.t(TAG).d(vin);
                        Call<VinQueryResponse> vinQueryResponseCall = mHachi.queryByVin(vin);
                        Response<VinQueryResponse> response = vinQueryResponseCall.execute();
                        VinQueryResponse vinQueryResponse = response.body();
                        Logger.t(TAG).d(response.code() + response.message());
                        if (vinQueryResponse != null) {
                            mAutoDetected = true;
                            mVehicleMaker = vinQueryResponse.makerName;
                            mVehicleModel = vinQueryResponse.modelName;
                            mVehicleYear = vinQueryResponse.year;

                            Logger.t(TAG).d("vin query response:" + vinQueryResponse.makerName + vinQueryResponse.modelName + vinQueryResponse.year);
                        }

                    } catch (IOException e) {
                        Logger.t(TAG).d(e.getMessage());
                    }
                }
                RawDataLoader mRawDataLoader = new RawDataLoader(mPlaylistEditor.getPlaylistId(), mVdbRequestQueue);
                try {
                    RawDataBlock rawDataBlock = mRawDataLoader.loadRawData(clipSet.getClip(0), RawDataItem.DATA_TYPE_GPS);
                    GpsData gpsData = (GpsData) rawDataBlock.getRawDataItem(0).data;
                    double lat = gpsData.coord.lat;
                    double lng = gpsData.coord.lng;
                    mGeoInfo.latitude = lat;
                    mGeoInfo.longitude = lng;
                    Call<GeoInfoResponse> geoInfoResponseCall = mHachi.getGeoInfo(lng, lat);
                    Response<GeoInfoResponse> response = geoInfoResponseCall.execute();
                    if (response.isSuccessful()) {
                        mLocation = response.body().getLocationString();
                        mGeoInfo.city = response.body().city;
                        mGeoInfo.country = response.body().country;
                        mGeoInfo.region = response.body().region;
                    }
                } catch (NullPointerException e) {
                    Logger.t(TAG).d(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<Clip>() {
            @Override
            public void onCompleted() {
                if (mVehicleMaker != null) {
                    mTvUserVehicleInfo.setText(mVehicleMaker + " " + mVehicleModel + " " + mVehicleYear);
                }
                if (mLocation != null) {
                    mTvGeoInfo.setText(mLocation);
                }
                if (!TextUtils.isEmpty(mTvVehicleInfo.getText()) && !TextUtils.isEmpty(mTvGeoInfo.getText())) {
                    mInfoSeparator.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Throwable e) {
                Logger.t(TAG).d(e.getMessage());

            }

            @Override
            public void onNext(Clip clip) {
                Logger.t(TAG).d("typeRace:" + clip.typeRace);
                Logger.t(TAG).d("clip raceTimingPoints:" + clip.raceTimingPoints.get(0));
                //mRaceLayout.setVisibility(View.VISIBLE);
                switch (clip.typeRace & Clip.MASK_RACE) {
                    case Clip.TYPE_RACE_AU3T:
                        momentType = "RACING_AU3T";
                        break;
                    case Clip.TYPE_RACE_AU6T:
                        momentType = "RACING_AU6T";
                        break;
                    case Clip.TYPE_RACE_CD3T:
                        momentType = "RACING_CD3T";
                        break;
                    case Clip.TYPE_RACE_CD6T:
                        momentType = "RACING_CD6T";
                        break;
                    default:
                        break;
                }
                for (long time : timingPoints) {
                    Logger.t(TAG).d(time);
                }
                Logger.t(TAG).d(clip.getStartTimeMs());
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
        Glide.with(this)
            .load(sessionManager.getAvatarUrl())
            .placeholder(R.drawable.menu_profile_photo_default)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(mUserAvatar);
        mUserName.setText(sessionManager.getUserName());


        Logger.t(TAG).d("is linked with facebook: " + sessionManager.getIsLinked());
//        if (sessionManager.isLinked()) {
//            mBtnFaceBook.setVisibility(View.VISIBLE);
//        } else {
//            mBtnFaceBook.setVisibility(View.GONE);
//        }

//        checkLinkedAccount();

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

        getToolbar().setTitleTextColor(getResources().getColor(R.color.app_text_color_primary));
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

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

        String descrption = mEtMomentDescription.getEditableText().toString();
        String[] tags = new String[]{};

        Map<String, String> gaugeSettings = GaugeSettingManager.getManager().getGaugeSettingMap();


        Logger.t(TAG).d("share name: " + title);

        LocalMoment localMoment = new LocalMoment(mPlaylistEditor.getPlaylistId(), title, descrption,
            tags, mSocialPrivacy, mAudioId, gaugeSettings, mIsFacebookShareChecked, mIsYoutubeShareChecked, cache);
        if (momentType != null && momentType.startsWith("RACING")) {
            Logger.t(TAG).d(momentType);
            String vehicleDescription = mVehicleDesc.getEditableText().toString();
            localMoment.momentType = momentType;
            localMoment.mVehicleDesc = vehicleDescription;
            localMoment.mTimingPoints = timingPoints;
        } else {
            ClipSet clipSet = getClipSet();
            if (clipSet.getCount() > 1) {
                localMoment.momentType = "NORMAL_MULTI";
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
        BgJobHelper.uploadMoment(localMoment);
        UploadingMomentActivity.launch(this);
        finish();
//
    }


}
