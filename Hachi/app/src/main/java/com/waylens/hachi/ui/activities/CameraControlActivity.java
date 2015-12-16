package com.waylens.hachi.ui.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.annotations.Sprite;
import com.mapbox.mapboxsdk.annotations.SpriteFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.orhanobut.logger.Logger;
import com.transee.ccam.BtState;
import com.transee.ccam.CameraState;
import com.transee.common.DateTime;
import com.transee.common.GPSRawData;
import com.waylens.hachi.views.camerapreview.CameraLiveView;
import com.transee.common.Timer;
import com.transee.vdb.PlaylistSet;
import com.transee.vdb.RemoteVdbClient;
import com.transee.vdb.Vdb;
import com.transee.vdb.VdbClient;
import com.transee.viditcam.actions.DialogBuilder;
import com.transee.viditcam.app.CameraSetupActivity;
import com.transee.viditcam.app.ViditImageButton;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.VolleyUtil;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipDownloadInfo;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.views.BarView;
import com.waylens.hachi.views.GForceView;
import com.waylens.hachi.views.GaugeView;
import com.waylens.hachi.views.GearView;

import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.Random;

public class CameraControlActivity extends com.transee.viditcam.app.BaseActivity {

    static final boolean DEBUG = false;
    static final String TAG = "MjpegActivity";

    static final int TIMER_IDLE = 0;
    static final int TIMER_SCHEDULING = 1;
    static final int TIMER_RUNNING = 2;

    static final int STATE_LOOP = 0;
    static final int STATE_MUTE = 1;
    static final int STATE_BATTERY = 2;

    static final int STILLCAP_STATE_IDLE = 0;
    static final int STILLCAP_STATE_WAITING = 1; // pressed
    static final int STILLCAP_STATE_BURSTING = 2;

    public static final int GPS_DEVICE = 0;
    public static final int GPS_CAMERA = 1;

    private boolean mbToolbarVisible;

    private VdtCamera mVdtCamera;
    private FrameLayout mMjpegViewHolder;
    private CameraLiveView mMjpegView;
    private Timer mUpdateTimer;
    private Timer mStillCaptureTimer;

    private int mStillCaptureState = STILLCAP_STATE_IDLE;

    private ViditImageButton mVideoButton;
    private ViditImageButton mSetupButton;
    private ViditImageButton mRecordButton;
    private ImageView mBtnBookmark;

    private View mModeView; // video or picture
    private RadioButton mVideoModeButton;
    private RadioButton mPictureModeButton;
    private TextView mTextRecordState;
    private RemoteVdbClient mRemoteVdbClient;

    GaugeView mGaugeView;
    GearView mGearView;
    BarView mWhp;
    BarView mPsi;
    BarView mBaro;
    GForceView mGForceView;
    View mOverlayView;

    MapView mapView;

    View mapHolder;
    ImageView weatherIcon;
    TextView weatherTemp;
    TextView weatherWind;
    TextView modeMsg;

    private Handler mHandler;
    private Random mRandom = new Random();

    Handler taskHandler = new Handler();
    private Runnable simulateDataTask = new Runnable() {
        @Override
        public void run() {
            simulateData();
            taskHandler.postDelayed(simulateDataTask, 1000);
        }
    };

    private PolylineOptions mPolylineOptions;
    private MarkerOptions mMarkerOptions;

    LocationManager locationManager;
    LocationListener locationListener;
    int gpsSource = GPS_DEVICE;

    private RequestQueue mRequestQueue;

    private static final String IS_PC_SERVER = "isPcServer";
    private static final String SSID = "ssid";
    private static final String HOST_STRING = "hostString";

    public static void launch(Context context, VdtCamera camera) {
        Intent intent = new Intent(context, CameraControlActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_PC_SERVER, camera.isPcServer());
        bundle.putString(SSID, camera.getSSID());
        bundle.putString(HOST_STRING, camera.getHostString());
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    @Override
    protected void requestContentView() {
        setContentView(R.layout.activity_camera_control);
    }

    @SuppressLint("InflateParams")
    @Override
    protected void onCreateActivity(Bundle savedInstanceState) {
//        mMjpegView = (CameraLiveView) getLayoutInflater().inflate(R.layout.group_camera_preview, null);
//        mMjpegView.setLayoutParams(new FrameLayout.LayoutParams(0, 0));

        mUpdateTimer = new Timer() {
            @Override
            public void onTimer(Timer timer) {
                if (mVdtCamera != null) {
//                    updateRecordTime();
                }
            }
        };

        mStillCaptureTimer = new Timer() {
            @Override
            public void onTimer(Timer timer) {
//                onStillCaptureTimer();
            }
        };
        mHandler = new Handler();
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(this);
    }

    private void startOverlay() {
        modeMsg.setVisibility(View.GONE);
        taskHandler.removeCallbacks(simulateDataTask);
    }

    private void simulateData() {
        float rpm = mRandom.nextInt(101) / 10.0f;
        mGaugeView.setTargetValue(rpm);
        int gear = mRandom.nextInt(7);
        mGaugeView.setSpeed(rpm * gear * 10);

        mGearView.setTargetValue(gear);
        mWhp.setTargetValue(mRandom.nextInt(900) + 100);
        mPsi.setTargetValue(mRandom.nextInt(65) - 30);
        //psi.setTargetValue(-13);
        mBaro.setTargetValue((mRandom.nextInt(70) + 260) / 10.0f);
        mGForceView.setUpValue(mRandom.nextInt(101) / 100.0f);
        mGForceView.setDownValue(mRandom.nextInt(101) / 100.0f);
        mGForceView.setLeftValue(mRandom.nextInt(101) / 100.0f);
        mGForceView.setRightValue(mRandom.nextInt(101) / 100.0f);
    }

    @Override
    protected void onStartActivity() {
        mVdtCamera = getCameraFromIntent(null);
        if (mVdtCamera != null) {

            InetSocketAddress serverAddr = mVdtCamera.getPreviewAddress();
            if (serverAddr == null) {
                mVdtCamera = null;
            } else {
                Logger.t(TAG).d("Start stream serverAddr: " + serverAddr);
                mVdtCamera.setOnStateChangeListener(mOnStateChangeListener);
                mMjpegView.startStream(serverAddr, new MyMjpegViewCallback(), true);
            }

            mVdtCamera.startPreview();
            mVdtCamera.getRecordRecMode();
            mVdtCamera.getCameraTime();
            mVdtCamera.getAudioMicState();
            mVdtCamera.getRecordResolutionList();
            mVdtCamera.GetSetup();

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startVdbClient();
                }
            }, 1000);
        }
        if (mVdtCamera == null) {
            noCamera();
        }
    }

    private void startVdbClient() {
        if (!showGauge()) {
            Log.e("test", "OBD is not connected!");
            //return;
        } else {
            Log.e("test", "OBD is connected!");
        }

        VdbClient.Callback vdbClientCallback = new VdbClient.Callback() {
            @Override
            public void onConnectionErrorAsync() {
                Log.e(TAG, "onConnectionErrorAsync");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //mGaugeViewHolder.setVisibility(View.GONE);
                        //mMjpegView.setAnchorTopThumbnail(0);
                    }
                });
            }

            @Override
            public void onVdbMounted() {
                Log.e(TAG, "onVdbMounted");
            }

            @Override
            public void onVdbUnmounted() {
                Log.e(TAG, "onVdbUnmounted");
            }

            @Override
            public void onClipSetInfoAsync(ClipSet clipSet) {
                Log.e(TAG, "onClipSetInfoAsync");
            }

            @Override
            public void onPlaylistSetInfoAsync(PlaylistSet playlistSet) {
                Log.e(TAG, "onPlaylistSetInfoAsync");
            }

            @Override
            public void onImageDataAsync(ClipPos clipPos, byte[] data) {
                Log.e(TAG, "onImageDataAsync");
            }

            @Override
            public void onBitmapDataAsync(ClipPos clipPos, Bitmap bitmap) {
                Log.e(TAG, "onBitmapDataAsync");
            }

            @Override
            public void onPlaylistIndexPicDataAsync(ClipPos posterPoint, byte[] data) {
                Log.e(TAG, "onPlaylistIndexPicDataAsync");
            }

            @Override
            public void onDownloadUrlFailedAsync() {
                Log.e(TAG, "onDownloadUrlFailedAsync");
            }

            @Override
            public void onDownloadUrlReadyAsync(ClipDownloadInfo downloadInfom, boolean bFirstLoop) {
                Log.e(TAG, "onDownloadUrlReadyAsync");
            }

            @Override
            public void onPlaybackUrlReadyAsync(PlaybackUrl playbackUrl) {
                Log.e(TAG, "onPlaybackUrlReadyAsync");
            }

            @Override
            public void onGetPlaybackUrlErrorAsync() {
                Log.e(TAG, "onGetPlaybackUrlErrorAsync");
            }

            @Override
            public void onPlaylistPlaybackUrlReadyAsync(VdbClient.PlaylistPlaybackUrl playlistPlaybackUrl) {
                Log.e(TAG, "onPlaylistPlaybackUrlReadyAsync");
            }

            @Override
            public void onGetPlaylistPlaybackUrlErrorAsync() {
                Log.e(TAG, "onGetPlaylistPlaybackUrlErrorAsync");
            }

            @Override
            public void onMarkClipResultAsync(int error) {
                Log.e(TAG, "onMarkClipResultAsync");
            }

            @Override
            public void onDeleteClipResultAsync(int error) {
                Log.e(TAG, "onDeleteClipResultAsync");
            }

            @Override
            public void onInsertClipResultAsync(int error) {
                Log.e(TAG, "onInsertClipResultAsync");
            }

            @Override
            public void onClipInfoAsync(int action, boolean isLive, Clip clip) {
                Log.e(TAG, "onClipInfoAsync");
            }

            @Override
            public void onMarkLiveClipInfo(int action, Clip clip, Vdb.MarkLiveInfo info) {
                Log.e(TAG, "onMarkLiveClipInfo");
            }

            @Override
            public void onClipRemovedAsync(Clip.ID cid) {
                Log.e(TAG, "onClipRemovedAsync");
            }

            @Override
            public void onPlaylistClearedAsync(int playlistId) {
                Log.e(TAG, "onPlaylistClearedAsync");
            }

            @Override
            public void onDownloadFinished(int id, String outputFile) {
                Log.e(TAG, "onDownloadFinished");
            }

            @Override
            public void onDownloadStarted(int id) {
                Log.e(TAG, "onDownloadStarted");
            }

            @Override
            public void onDownloadError(int id) {
                Log.e(TAG, "onDownloadError");
            }

            @Override
            public void onDownloadProgress(int id, int progress) {
                Log.e(TAG, "onDownloadProgress");
            }

            @Override
            public void onRawDataResultAsync(RawData rawDataResult) {
                Log.e(TAG, "onRawDataResultAsync");
            }

            @Override
            public void onRawDataAsync(int dataType, byte[] data) {
                if (dataType == RawDataBlock.RAW_DATA_GPS
                        && gpsSource == GPS_CAMERA) {

                    try {
                        final GPSRawData gpsRawData = GPSRawData.translate(data);
                        Log.e("gpsdata", "lat, lon: " + gpsRawData.coord.lat_orig + ", " + gpsRawData.coord.lng_orig);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                updateMap(gpsRawData);
                                getWeatherData(gpsRawData.coord.lat_orig, gpsRawData.coord.lng_orig);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "GPS Error", e);
                    }

                }

                if (dataType == RawDataBlock.RAW_DATA_ODB) {
                    final OBDData obdData = OBDData.parse(data);
                    if (obdData == null) {
                        return;
                    }
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            //Log.e(TAG,"Bottom: " + mGaugeViewHolder.getBottom());
                            mGaugeView.setSpeed(obdData.speed);
                            mGaugeView.setTargetValue(obdData.rpm / 1000);

                        }
                    });
                }
            }

            @Override
            public void onRawDataBlockAsync(RawDataBlock block) {
                Log.e(TAG, "onRawDataBlockAsync");
            }

            @Override
            public void onDownloadRawDataBlockAsync(RawDataBlock.DownloadRawDataBlock block) {
                Log.e(TAG, "onDownloadRawDataBlockAsync");
            }

            @Override
            public void onBufferSpaceLowAsync(RemoteVdbClient.BufferSpaceLowInfo info) {
                Log.e(TAG, "onBufferSpaceLowAsync");
            }

            @Override
            public void onBufferFullAsync() {
                Log.e(TAG, "onBufferFullAsync");
            }
        };
        String path = Environment.getExternalStoragePublicDirectory("richard-vidit").getAbsolutePath();
        if (mRemoteVdbClient == null) {
            mRemoteVdbClient = new RemoteVdbClient(vdbClientCallback, path);
            String hostString = getHostString(mVdtCamera);
            if (hostString != null) {
                mRemoteVdbClient.start(hostString);
                //VdbClient.F_RAW_DATA_GPS + VdbClient.F_RAW_DATA_ACC
                mRemoteVdbClient.requestSetDrawDataOption(RawDataBlock.F_RAW_DATA_ODB + RawDataBlock.F_RAW_DATA_GPS);
            }
        }
    }

    private void updateMap(GPSRawData gpsRawData) {
        if (mapView == null || mMarkerOptions == null || mPolylineOptions == null) {
            return;
        }
        LatLng latLng = new LatLng(gpsRawData.coord.lat_orig, gpsRawData.coord.lng_orig);
        mPolylineOptions.add(latLng);
        mMarkerOptions.position(latLng);
        mapView.setCenterCoordinate(latLng);
    }

    private void updateMap(Location location) {
        if (mapView == null || mMarkerOptions == null || mPolylineOptions == null) {
            return;
        }
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mPolylineOptions.add(latLng);
        mMarkerOptions.position(latLng);
        mapView.setCenterCoordinate(latLng);

    }

    private void getLocation() {
        if (gpsSource != GPS_DEVICE) {
            return;
        }
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                //Log.e("test", "lat: " + location.getLatitude());
                //Log.e("test", "lng: " + location.getLongitude());
                updateMap(location);
                getWeatherData(location.getLatitude(), location.getLongitude());
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

    }


    private boolean showGauge() {
        if (mVdtCamera == null) {
            return false;
        }

        BtState btState = mVdtCamera.getBtStates();
        return (btState.mBtState == BtState.BT_State_Enabled)
                && (btState.mObdState.mState == BtState.BTDEV_State_On);
    }

    private String getHostString(VdtCamera vdtCamera) {
        String hostString = null;
        Bundle bundle = getIntent().getExtras();
        if (isServerActivity(bundle)) {
            hostString = getServerAddress(bundle);
        } else {
            if (vdtCamera != null) {
                hostString = vdtCamera.getHostString();
            }
        }
        return hostString;
    }

    @Override
    protected void onStopActivity() {
        if (mUpdateTimer != null) {
            mUpdateTimer.cancel();
        }
        mMjpegView.stopStream();
        removeCamera();
        if (mRemoteVdbClient != null) {
            mRemoteVdbClient.stop();
            mRemoteVdbClient = null;
        }
        taskHandler.removeCallbacks(simulateDataTask);
        if (locationManager != null && locationListener != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }
            locationManager.removeUpdates(locationListener);
        }

        mapView.onStop();

    }

    @Override
    public void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        startOverlay();
        getLocation();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onReleaseUI() {
        mMjpegViewHolder.removeAllViews();
    }

    @SuppressLint("InlinedApi")
    @Override
    protected void onInitUI() {

        cancelToolbarTimer();

        if (isLandscape()) {
            mbToolbarVisible = true;
            startToolbarTimer();
        }

        mMjpegViewHolder = (FrameLayout) findViewById(R.id.mjpegViewHolder);
//        Utils.setViewFullSize(this, mMjpegView);
//        mMjpegViewHolder.addView(mMjpegView);

        mMjpegView = (CameraLiveView)findViewById(R.id.mjpegView1);

        mVideoButton = (ViditImageButton) findViewById(R.id.btnVideo);
        mVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVdtCamera.setCameraWantIdle();
                //startCameraActivity(mVdtCamera, CameraVideoActivity.class);
            }
        });

        mRecordButton = (ViditImageButton) findViewById(R.id.imageButton1);
        mRecordButton.setVisibility(View.VISIBLE);

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickRecordButton();
            }
        });

        mRecordButton.setOnPressedListener(new ViditImageButton.OnPressedListener() {
            @Override
            public void onPressed(ViditImageButton button) {
                onRecordButtonPressed();
            }
        });

        mBtnBookmark = (ImageView) findViewById(R.id.btn_bookmark);
        mBtnBookmark.getDrawable().setColorFilter(getResources().getColor(R.color.style_color_primary), PorterDuff.Mode.MULTIPLY);
        mBtnBookmark.setVisibility(View.GONE);
        mBtnBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addBookmark();
            }
        });

        mSetupButton = (ViditImageButton) findViewById(R.id.btnSetup);
        mSetupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVdtCamera != null) {
                    startCameraActivity(mVdtCamera, CameraSetupActivity.class);
                }
            }
        });

        View recordState = findViewById(R.id.recordState1);
        mModeView = findViewById(R.id.radioGroup1);
        mVideoModeButton = (RadioButton) findViewById(R.id.radio0);
        mPictureModeButton = (RadioButton) findViewById(R.id.radio1);
        mTextRecordState = (TextView) recordState.findViewById(R.id.textView1);

        mVideoModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickVideoMode();
            }
        });

        mPictureModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickPictureMode();
            }
        });

        if (isLandscape()) {
            mMjpegView.setAnchorRect(0, 0, 0, 0, 2.0f);
        } else {
            mMjpegView.setAnchorRect(0, 0, 0, 0, -1.0f);
        }

        mMjpegView.setAnchorTopThumbnail(0);

        //toggleFullScreen();

        // position the Record button
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mRecordButton.getLayoutParams();
        switch (rotation) {
            default:
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                break;
            case Surface.ROTATION_90:
                lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
                try {
                    lp.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
                    lp.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
                } catch (Exception e) {
                }
                break;
            case Surface.ROTATION_270:
                lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                try {
                    lp.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
                    lp.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
                } catch (Exception e) {
                }
                break;
        }
        mRecordButton.setLayoutParams(lp);


        mGaugeView = (GaugeView) findViewById(R.id.gauge_view);
        mGearView = (GearView) findViewById(R.id.gear_view);
        mWhp = (BarView) findViewById(R.id.view_whp);
        mPsi = (BarView) findViewById(R.id.view_psi);
        mBaro = (BarView) findViewById(R.id.view_baro);
        mGForceView = (GForceView) findViewById(R.id.gforce_view);
        mOverlayView = findViewById(R.id.overlay_view);

        mapView = (MapView) findViewById(R.id.mapbox_view);

        mapView.setZoomLevel(11);
        mapView.setStyleUrl(Style.DARK);


        LatLng aCenter = new LatLng(31.190979000000002, 121.60145658333334);
        mapView.setCenterCoordinate(aCenter);
        SpriteFactory spriteFactory = new SpriteFactory(mapView);
        Sprite icon = spriteFactory.fromResource(R.drawable.map_car_inner_red_triangle);
        mMarkerOptions = new MarkerOptions().position(aCenter).icon(icon);
        mapView.addMarker(mMarkerOptions);

        mPolylineOptions = new PolylineOptions().color(Color.rgb(252, 219, 12)).width(3).add(aCenter);
        mapView.addPolyline(mPolylineOptions);
        mapView.onCreate(null);

        mapHolder = findViewById(R.id.map_holder);
        weatherIcon = (ImageView) findViewById(R.id.weather_icon);
        weatherTemp = (TextView) findViewById(R.id.weather_temp);
        weatherWind = (TextView) findViewById(R.id.weather_wind);
        modeMsg = (TextView) findViewById(R.id.mode_msg);
        modeMsg.setVisibility(View.GONE);
        initWeatherData();
    }

    private void addBookmark() {
        CameraState states = mVdtCamera.getState();
        if (states.mRecordState != CameraState.State_Record_Recording) {
            return;
        }
        mBtnBookmark.setEnabled(false);
        mBtnBookmark.getDrawable().setColorFilter(getResources().getColor(R.color.material_grey_600), PorterDuff.Mode.MULTIPLY);
        mVdtCamera.markLiveVideo();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBtnBookmark.setEnabled(true);
                mBtnBookmark.getDrawable().setColorFilter(getResources().getColor(R.color.style_color_primary), PorterDuff.Mode.MULTIPLY);
            }
        }, states.mMarkAfterTime * 1000);

    }

    private void initWeatherData() {
        String tempF = PreferenceUtils.getString(PreferenceUtils.KEY_WEATHER_TEMP_F, null);

        String windSpeedKmph = PreferenceUtils.getString(PreferenceUtils.KEY_WEATHER_WIND_SPEED, null);

        String iconUrl = PreferenceUtils.getString(PreferenceUtils.KEY_WEATHER_ICON_URL, null);
        if (tempF == null) {
            return;
        }
        weatherTemp.setText(tempF);
        weatherWind.setText(windSpeedKmph);
        ImageLoader.getInstance().displayImage(iconUrl, weatherIcon);
    }

    private void getWeatherData(double lat, double lng) {
        long updateTime = PreferenceUtils.getLong(PreferenceUtils.KEY_WEATHER_UPDATE_TIME, 0);
        long now = System.currentTimeMillis();
        Log.e("test", "now:" + now);
        Log.e("test", "updateTime:" + updateTime);
        if (now - updateTime < 1000 * 60 * 1) {
            Log.e("test", "Don't need to update weather data");
            return;
        }

        String url = String.format(Constants.API_WEATHER, lat + "," + lng);
        mRequestQueue.add(new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject current = response.getJSONObject("data").getJSONArray("current_condition").getJSONObject(0);
                            if (mapHolder == null) {
                                return;
                            }

                            String tempF = current.optString("temp_F") + "\u00B0";
                            String windSpeedKmph = current.optString("windspeedKmph") + "Kmph";
                            String iconUrl = current.getJSONArray("weatherIconUrl").getJSONObject(0).getString("value");
                            PreferenceUtils.putString(PreferenceUtils.KEY_WEATHER_TEMP_F, tempF);
                            PreferenceUtils.putString(PreferenceUtils.KEY_WEATHER_WIND_SPEED, windSpeedKmph);
                            PreferenceUtils.putString(PreferenceUtils.KEY_WEATHER_ICON_URL, iconUrl);
                            PreferenceUtils.putLong(PreferenceUtils.KEY_WEATHER_UPDATE_TIME, System.currentTimeMillis());
                            weatherTemp.setText(tempF);
                            weatherWind.setText(windSpeedKmph);
                            ImageLoader.getInstance().displayImage(iconUrl, weatherIcon);
                        } catch (Exception e) {
                            Log.e("test", "", e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("test", "Error: " + error);
                    }
                }));

        mRequestQueue.start();

        //String q = "31.190979000000002,121.60145658333334";
        //service.getWeather("e081e88edf6ffe4bcd0d12f34b26e", "json", "1", "12", q, callback);
    }

    @Override
    protected void onSetupUI() {
        //mMjpegView.setBackgroundColor(Color.BLACK);
//        if (isPortrait()) {
//            mMjpegView.setThumbnailScale(3);
//            mMjpegView.setBackgroundColor(getResources().getColor(R.color.previewBackground));
//        } else {
//            mMjpegView.setThumbnailScale(4);
//            mMjpegView.setBackgroundColor(Color.BLACK);
//        }
        //updateRecordState();
    }

    private void noCamera() {
        if (DEBUG) {
            Log.d(TAG, "camera not found or disconnected");
        }
        // TODO - if dialog/popupmenu exists
        performFinish();
    }

    private void removeCamera() {
        if (mVdtCamera != null) {
            //mVdtCamera.removeCallback(mCameraCallback);
            mVdtCamera = null;
        }
    }

    private final void cancelToolbarTimer() {
    }

    private final void startToolbarTimer() {
    }

    private void toggleToolbar() {
        cancelToolbarTimer();
        mbToolbarVisible = !mbToolbarVisible;
        updateToolbarVisibility();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean result = super.dispatchTouchEvent(ev);
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            cancelToolbarTimer();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (isLandscape() && mbToolbarVisible) {
                startToolbarTimer();
            }
        }
        return result;
    }

    private void onClickRecordButton() {
        CameraState states = mVdtCamera.getState();
        if (states.mRecordState == CameraState.State_Record_Recording) {
            mVdtCamera.stopRecording();
        } else if (states.mRecordState == CameraState.State_Record_Stopped) {
            if (!states.mbIsStill) {
                mVdtCamera.startRecording();
            } else {
                switch (mStillCaptureState) {
                    case STILLCAP_STATE_IDLE:
                        break;
                    case STILLCAP_STATE_WAITING:
                        mStillCaptureState = STILLCAP_STATE_IDLE;
                        mVdtCamera.startStillCapture(true);
                        //mMjpegView.startMask(5);
                        //BeepManager.play(thisApp, R.raw.beep2, false);
                        mStillCaptureTimer.cancel();
                        break;
                    case STILLCAP_STATE_BURSTING:

                        mVdtCamera.stopStillCapture();
                        mStillCaptureState = STILLCAP_STATE_IDLE;
                        break;
                }
            }
        }
    }

    // set a timer to do still capture
    private void onRecordButtonPressed() {
        CameraState states = mVdtCamera.getState();
        if (!states.canDoStillCapture())
            return;

        if (!states.mbIsStill)
            return;

        if (states.mRecordState == CameraState.State_Record_Stopped) {
            if (mStillCaptureState == STILLCAP_STATE_IDLE) {
                mStillCaptureState = STILLCAP_STATE_WAITING;
                mStillCaptureTimer.run(300);
            }
        }
    }

    private void onStillCaptureTimer() {
        // timeout, begin bursting
        if (mStillCaptureState == STILLCAP_STATE_WAITING) {
            if (mVdtCamera != null) {
                mVdtCamera.startStillCapture(false);
            }
            mStillCaptureState = STILLCAP_STATE_BURSTING;
        }
    }

    private void updateRecordTime() {
        CameraState states = mVdtCamera.getState();
        if (states.mRecordState == CameraState.State_Record_Recording) {
            if (mUpdateTimer.tag == TIMER_IDLE) {
                states.mRecordDuration = -2; // TODO
                states.mbRecordDurationUpdated = false;
                mVdtCamera.getCameraTime();
                mUpdateTimer.tag = TIMER_SCHEDULING;
            } else if (mUpdateTimer.tag == TIMER_SCHEDULING) {
                if (states.mbRecordDurationUpdated) {
                    mUpdateTimer.tag = TIMER_RUNNING;
                }
            }
            if (mUpdateTimer.tag == TIMER_RUNNING) {
                long elapsed = SystemClock.uptimeMillis() - states.mRecordTimeFetchedTime;
                int recordDuration = states.mRecordDuration + (int) (elapsed / 1000);
                String text = DateTime.secondsToString(recordDuration);
                mTextRecordState.setText(text);
            } else {
                String text = DateTime.secondsToString(0);
                mTextRecordState.setText(text);
            }
            mUpdateTimer.run(1000);
        } else {
            mUpdateTimer.tag = TIMER_IDLE;
            mUpdateTimer.cancel();
        }
    }

    private void updateToolbarVisibility() {
        if (isLandscape()) {
            if (mbToolbarVisible) {
                mVideoButton.setVisibility(View.GONE);
                mSetupButton.setVisibility(View.GONE);
                mOverlayView.setVisibility(View.VISIBLE);
                mapHolder.setVisibility(View.VISIBLE);
                startToolbarTimer();
            } else {
                mVideoButton.setVisibility(View.GONE);
                mSetupButton.setVisibility(View.GONE);
                mOverlayView.setVisibility(View.GONE);
                mapHolder.setVisibility(View.GONE);
                //mGaugeViewHolder.setVisibility(View.GONE);
            }
        }
        updateRecordButton();
    }

    private void updateRecordButton() {
        int visibility = isLandscape() && !mbToolbarVisible ? View.GONE : View.VISIBLE;
        CameraState states = mVdtCamera.getState();
        boolean bEnable = true;
        switch (states.mRecordState) {
            default:
            case CameraState.State_Record_Unknown:
                mRecordButton.setVisibility(View.GONE);
                mBtnBookmark.setVisibility(View.GONE);
                bEnable = false;
                break;
            case CameraState.State_Record_Stopped:
                mRecordButton.changeImages(R.drawable.start_record, R.drawable.start_record_pressed);
                mRecordButton.setVisibility(visibility);
                mBtnBookmark.setVisibility(View.GONE);
                break;
            case CameraState.State_Record_Stopping:
                bEnable = false;
                break;
            case CameraState.State_Record_Starting:
                bEnable = false;
                break;
            case CameraState.State_Record_Recording:
                mRecordButton.changeImages(R.drawable.stop_record, R.drawable.stop_record_pressed);
                mRecordButton.setVisibility(visibility);
                mBtnBookmark.setVisibility(visibility);
                break;
            case CameraState.State_Record_Switching:
                bEnable = false;
                break;
        }
        mRecordButton.setClickable(bEnable);
    }

    private void showCameraState(int index, int resId) {
        if (mMjpegView.getStateResId(index) != resId) {
            Drawable d = getResources().getDrawable(resId);
            mMjpegView.setState(index, resId, d);
        }
    }

    private void hideCameraState(int index) {
        if (mMjpegView.getStateResId(index) != -1) {
            mMjpegView.setState(index, -1, null);
        }
    }

    private void updateModeState() {
        CameraState states = mVdtCamera.getState();
        if (states.canDoStillCapture()
                && (states.mRecordState == CameraState.State_Record_Stopped || states.mRecordState == CameraState.State_Record_Switching)) {
            mModeView.setVisibility(View.VISIBLE);
            mTextRecordState.setText("");
            if (states.mbIsStill) {
                mPictureModeButton.setChecked(true);
            } else {
                mVideoModeButton.setChecked(true);
            }
        } else {
            mModeView.setVisibility(View.GONE);
        }
    }

    private void setRecordStateDrawable(int drawable) {
        mTextRecordState.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
    }

    private void updateRecordState() {
        CameraState states = mVdtCamera.getState();

        switch (states.mRecordState) {
            case CameraState.State_Record_Stopped:
                if (mStillCaptureState == STILLCAP_STATE_IDLE) {
                    setRecordStateDrawable(0);
                    mTextRecordState.setText("");
                }
                break;
            case CameraState.State_Record_Stopping:
                setRecordStateDrawable(0);
                mTextRecordState.setText(R.string.info_stop_recording);
                break;
            case CameraState.State_Record_Starting:
                setRecordStateDrawable(0);
                mTextRecordState.setText(R.string.info_start_recording);
                break;
            case CameraState.State_Record_Recording:
                setRecordStateDrawable(R.drawable.recording);
                break;
            default:
                setRecordStateDrawable(0);
                mTextRecordState.setText("");
                mModeView.setVisibility(View.GONE);
                break;
        }

        if (states.mRecordState == CameraState.State_Record_Switching) {
            mMjpegView.startMask(1000);
        } else {
            mMjpegView.startMask(0);
        }

        updateRecordButton();
        updateRecordTime();
        updateModeState();

        // loop state
        // TODO: optimize
        if (states.mRecordModeIndex > 0 && (states.mRecordModeIndex & CameraState.FLAG_LOOP_RECORD) != 0) {
            showCameraState(STATE_LOOP, R.drawable.loop);
        } else {
            hideCameraState(STATE_LOOP);
        }

        // mic mute state
        // TODO: optimize
        if (states.mMicState > 0 && (states.mMicState == CameraState.State_Mic_MUTE)) {
            showCameraState(STATE_MUTE, R.drawable.mic_off);
        } else {
            hideCameraState(STATE_MUTE);
        }

        // batter state
        // TODO: optimize
        if (states.mBatteryVol <= 0) {
            hideCameraState(STATE_BATTERY);
        } else if (states.mBatteryVol < 25) {
            showCameraState(STATE_BATTERY, R.drawable.battery_low);
        } else if (states.mBatteryVol < 50) {
            showCameraState(STATE_BATTERY, R.drawable.battery_25);
        } else if (states.mBatteryVol < 75) {
            showCameraState(STATE_BATTERY, R.drawable.battery_50);
        } else if (states.mBatteryVol < 100) {
            showCameraState(STATE_BATTERY, R.drawable.battery_75);
        } else {
            showCameraState(STATE_BATTERY, R.drawable.battery_full);
        }
    }

    private void startBurst(int numPictures, int burstTicks) {
        setRecordStateDrawable(0);
        //String text = String.format(Locale.US, "%d (%d ms)", numPictures, burstTicks);
        //mTextRecordState.setText(text);
        mTextRecordState.setText(Integer.toString(numPictures));
        mModeView.setVisibility(View.GONE);
        // BeepManager.play(thisApp, R.raw.beep2, true);
    }

    private void stopBurst() {
        mTextRecordState.setText("");
        mModeView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        performFinish();
    }

    private void performFinish() {
        finish();

    }

    private void onMjpegConnectionError(int error) {
        if (DEBUG) {
            Log.d(TAG, "onMjpegConnectionError");
        }
        if (mMjpegView != null) {
            // TODO
        }
    }

    private void onStartRecordError(int error) {
        int msgId = 0;
        switch (error) {
            case CameraState.Error_StartRecord_OK:
                break;
            case CameraState.Error_StartRecord_NoCard:
                msgId = R.string.msg_record_error_no_card;
                break;
            case CameraState.Error_StartRecord_CardFull:
                msgId = R.string.msg_record_error_card_full;
                break;
            case CameraState.Error_StartRecord_CardError:
                msgId = R.string.msg_record_error_card_error;
                break;
            default:
                msgId = R.string.msg_record_error;
                break;
        }
        if (msgId != 0) {
            DialogBuilder action = new DialogBuilder(this);
            action.setMsg(msgId);
            action.setButtons(DialogBuilder.DLG_OK);
            action.show();
        }
    }

    private void onClickVideoMode() {
        CameraState states = mVdtCamera.getState();
        if (states.mRecordState == CameraState.State_Record_Stopped) {
            mVdtCamera.setRecordStillMode(false);
            updateModeState();
        }
    }

    private void onClickPictureMode() {
        CameraState states = mVdtCamera.getState();
        if (states.mRecordState == CameraState.State_Record_Stopped) {
            mVdtCamera.setRecordStillMode(true);
            updateModeState();
        }
    }

    private final VdtCamera.OnStateChangeListener mOnStateChangeListener = new VdtCamera.OnStateChangeListener() {
        @Override
        public void onStateChanged(VdtCamera vdtCamera) {
            if (vdtCamera == mVdtCamera) {
                updateRecordState();
            }
        }

        @Override
        public void onBtStateChanged(VdtCamera vdtCamera) {

        }

        @Override
        public void onGpsStateChanged(VdtCamera vdtCamera) {

        }

        @Override
        public void onWifiStateChanged(VdtCamera vdtCamera) {

        }
    };

    class MyMjpegViewCallback implements CameraLiveView.Callback {


        @Override
        public void onSingleTapUp() {
            toggleToolbar();
        }

        @Override
        public void onIoErrorAsync(final int error) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onMjpegConnectionError(error);
                }
            });
        }

    }
}
