package com.waylens.hachi.ui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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

import com.nostra13.universalimageloader.core.ImageLoader;
import com.transee.ccam.AbsCameraClient;
import com.transee.ccam.BtState;
import com.transee.ccam.CameraClient;
import com.transee.ccam.CameraState;
import com.transee.common.DateTime;
import com.transee.common.GPSRawData;
import com.transee.common.MjpegBitmap;
import com.transee.common.OBDData;
import com.transee.common.Timer;
import com.transee.common.Utils;
import com.transee.vdb.PlaylistSet;
import com.transee.vdb.RemoteVdbClient;
import com.transee.vdb.Vdb;
import com.transee.vdb.VdbClient;
import com.transee.viditcam.actions.DialogBuilder;
import com.transee.viditcam.app.CameraSetupActivity;
import com.transee.viditcam.app.ViditImageButton;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.DownloadInfoEx;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.views.BarView;
import com.waylens.hachi.views.GForceView;
import com.waylens.hachi.views.GaugeView;
import com.waylens.hachi.views.GearView;
import com.waylens.hachi.views.PrefsUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    private boolean mbToolbarVisible;

    private VdtCamera mVdtCamera;
    private FrameLayout mMjpegViewHolder;
    private MjpegBitmap mMjpegView;
    private Timer mUpdateTimer;
    private Timer mStillCaptureTimer;

    private int mStillCaptureState = STILLCAP_STATE_IDLE;

    private ViditImageButton mVideoButton;
    private ViditImageButton mSetupButton;
    private ViditImageButton mRecordButton;

    private View mModeView; // video or picture
    private RadioButton mVideoModeButton;
    private RadioButton mPictureModeButton;
    private TextView mTextRecordState;
    private RemoteVdbClient mRemoteVdbClient;

    //private GaugeView mGaugeSpeed;
    //private GaugeView mGaugeRpm;
    //private GaugeView mGaugeTemperature;
    //private View mGaugeViewHolder;

    GaugeView mGaugeView;
    GearView mGearView;
    BarView mWhp;
    BarView mPsi;
    BarView mBaro;
    GForceView mGForceView;
    View mOverlayView;

    //MapView mapView;

    View mapHolder;
    ImageView weatherIcon;
    TextView weatherTemp;
    TextView weatherWind;
    TextView modeMsg;

    private int mMode;
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

    //private PathOverlay pathOverlay;
    //private Marker marker;

    LocationManager locationManager;
    LocationListener locationListener;
    int gpsSource;

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
        mMjpegView = (MjpegBitmap) getLayoutInflater().inflate(R.layout.group_camera_preview, null);
        mMjpegView.setLayoutParams(new FrameLayout.LayoutParams(0, 0));

        mUpdateTimer = new Timer() {
            @Override
            public void onTimer(Timer timer) {
                if (mVdtCamera != null) {
                    updateRecordTime();
                }
            }
        };

        mStillCaptureTimer = new Timer() {
            @Override
            public void onTimer(Timer timer) {
                onStillCaptureTimer();
            }
        };
        PrefsUtil.initImageLoader(this);
        mHandler = new Handler();
    }

    private void startOverlay() {
        int mode = PrefsUtil.getDataMode();
        if (mode == PrefsUtil.MODE_SIMULATION) {
            modeMsg.setVisibility(View.VISIBLE);
            taskHandler.removeCallbacks(simulateDataTask);
            taskHandler.post(simulateDataTask);
        } else {
            modeMsg.setVisibility(View.GONE);
            taskHandler.removeCallbacks(simulateDataTask);
        }
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
                //mVdtCamera.addCallback(mCameraCallback);
                mMjpegView.startStream(serverAddr, new MyMjpegViewCallback(), true);
            }
            // mCamera.getClient().cmdGetResolution();
            // mCamera.getClient().cmdGetQuality();
            // mCamera.getClient().cmdGetColorMode();

            AbsCameraClient client = mVdtCamera.getClient();
            client.cmd_CAM_WantPreview();
            client.cmd_Rec_get_RecMode();
            client.ack_Cam_get_time();
            client.cmd_audio_getMicState();
            client.cmd_Rec_List_Resolutions(); // see if still capture is supported
            ((CameraClient) client).userCmd_GetSetup();
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
            public void onDownloadUrlReadyAsync(DownloadInfoEx downloadInfom, boolean bFirstLoop) {
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
            public void onRawDataResultAsync(VdbClient.RawDataResult rawDataResult) {
                Log.e(TAG, "onRawDataResultAsync");
            }

            @Override
            public void onRawDataAsync(int dataType, byte[] data) {
                if (dataType == VdbClient.RAW_DATA_GPS
                    && gpsSource == PrefsUtil.GPS_CAMERA) {
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

                if (mMode != PrefsUtil.MODE_REAL) {
                    return;
                }

                if (dataType == VdbClient.RAW_DATA_ODB) {
                    final OBDData obdData = Utils.parseOBD(data);
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
            public void onRawDataBlockAsync(VdbClient.RawDataBlock block) {
                Log.e(TAG, "onRawDataBlockAsync");
            }

            @Override
            public void onDownloadRawDataBlockAsync(VdbClient.DownloadRawDataBlock block) {
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
                mRemoteVdbClient.requestSetDrawDataOption(VdbClient.F_RAW_DATA_ODB + VdbClient.F_RAW_DATA_GPS);
            }
        }
    }

    private void updateMap(GPSRawData gpsRawData) {
        /*if (mapView == null || pathOverlay == null || marker == null) {
            return;
        }
        LatLng latLng = new LatLng(gpsRawData.coord.lat_orig, gpsRawData.coord.lng_orig);
        pathOverlay.addPoint(latLng);
        marker.setPoint(latLng);
        mapView.setCenter(latLng);
    */
    }

    private void updateMap(Location location) {
        /*
        if (mapView == null || pathOverlay == null || marker == null) {
            return;
        }
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        pathOverlay.addPoint(latLng);
        marker.setPoint(latLng);
        mapView.setCenter(latLng);
        */
    }

    private void getLocation() {
        if (gpsSource != PrefsUtil.GPS_DEVICE) {
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
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

    }


    private boolean showGauge() {
        if (mVdtCamera == null) {
            return false;
        }

        BtState btState = VdtCamera.getBtStates(mVdtCamera);
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
            //locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        startOverlay();
        getLocation();
    }

    @Override
    public void onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        Utils.setViewFullSize(this, mMjpegView);
        mMjpegViewHolder.addView(mMjpegView);

        mVideoButton = (ViditImageButton) findViewById(R.id.btnVideo);
        mVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVdtCamera.getClient().cmd_CAM_WantIdle();
                startCameraActivity(mVdtCamera, CameraVideoActivity.class);
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

        toggleFullScreen();

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

        //mGaugeSpeed = (GaugeView) findViewById(R.id.gauge_speed);
        //mGaugeRpm = (GaugeView) findViewById(R.id.gauge_rpm);
        //mGaugeTemperature = (GaugeView) findViewById(R.id.gauge_temperature);
        //mGaugeViewHolder = findViewById(R.id.gauge_view_holder);
        mGaugeView = (GaugeView) findViewById(R.id.gauge_view);
        mGearView = (GearView) findViewById(R.id.gear_view);
        mWhp = (BarView) findViewById(R.id.view_whp);
        mPsi = (BarView) findViewById(R.id.view_psi);
        mBaro = (BarView) findViewById(R.id.view_baro);
        mGForceView = (GForceView) findViewById(R.id.gforce_view);
        mOverlayView = findViewById(R.id.overlay_view);
        /*
        mapView = (MapView) findViewById(R.id.mapbox_view);
        mapView.setZoom(16);
        pathOverlay = new PathOverlay(Color.rgb(252, 219, 12), 3);
        mapView.addOverlay(pathOverlay);
        LatLng aCenter = new LatLng(31.190979000000002, 121.60145658333334);
        mapView.setCenter(aCenter);
        marker = mapView.addMarker(new Marker("Current", "Your current location", aCenter));
        Drawable icon = getResources().getDrawable(R.drawable.map_car_inner_red_triangle);
        marker.setIcon(new Icon(icon));
        mapView.addMarker(marker);
        */
        mapHolder = findViewById(R.id.map_holder);
        weatherIcon = (ImageView) findViewById(R.id.weather_icon);
        weatherTemp = (TextView) findViewById(R.id.weather_temp);
        weatherWind = (TextView) findViewById(R.id.weather_wind);
        modeMsg = (TextView) findViewById(R.id.mode_msg);
        mMode = PrefsUtil.getDataMode();
        if (mMode == PrefsUtil.MODE_REAL) {
            modeMsg.setVisibility(View.GONE);
        } else {
            modeMsg.setVisibility(View.VISIBLE);
        }
        gpsSource = PrefsUtil.getGPSource();
        initWeatherData();
        //getWeatherData(null);
    }

    private void initWeatherData() {
        String tempF = PrefsUtil.getWeatherTempF();
        String windSpeedKmph = PrefsUtil.getWeatherWindSpeed();
        String iconUrl = PrefsUtil.getWeatherIcon();
        if (tempF == null) {
            return;
        }
        weatherTemp.setText(tempF);
        weatherWind.setText(windSpeedKmph);
        ImageLoader.getInstance().displayImage(iconUrl, weatherIcon);
    }

    private void getWeatherData(double lat, double lng) {
        /*
        long updateTime = PrefsUtil.getUpdateWeatherTime();
        long now = System.currentTimeMillis();
        Log.e("test", "now:" + now);
        Log.e("test", "updateTime:" + updateTime);
        if (now - updateTime < 1000 * 60 * 1) {
            Log.e("test", "Don't need to update weather data");
            return;
        }
        RestAdapter restAdapter = new RestAdapter.Builder()
            .setEndpoint("http://api.worldweatheronline.com")
            .build();
        WeatherService service = restAdapter.create(WeatherService.class);
        Callback<Response> callback = new Callback<Response>() {
            @Override
            public void success(Response rawResponse, Response response) {
                try {
                    byte[] bytes = streamToBytes(rawResponse.getBody().in());
                    String result = new String(bytes);
                    JSONObject jsonObject = new JSONObject(result);
                    JSONObject current = jsonObject.getJSONObject("data").getJSONArray("current_condition").getJSONObject(0);
                    if (mapHolder == null) {
                        return;
                    }

                    String tempF = current.optString("temp_F") + "\u00B0";
                    String windspeedKmph = current.optString("windspeedKmph") + "Kmph";
                    String iconUrl = current.getJSONArray("weatherIconUrl").getJSONObject(0).getString("value");
                    PrefsUtil.setWeatherTempF(tempF);
                    PrefsUtil.setWeatherWindSpeed(windspeedKmph);
                    PrefsUtil.setWeatherIcon(iconUrl);
                    PrefsUtil.setUpdateWeatherTime(System.currentTimeMillis());
                    weatherTemp.setText(tempF);
                    weatherWind.setText(windspeedKmph);
                    ImageLoader.getInstance().displayImage(iconUrl, weatherIcon);
                } catch (Exception e) {
                    Log.e("test", "Error", e);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("test", "Error: " + error);
            }
        };
        //String q = "31.190979000000002,121.60145658333334";
        String q = lat + "," + lng;
        service.getWeather("e081e88edf6ffe4bcd0d12f34b26e", "json", "1", "12", q, callback);
        */
    }

    public static byte[] streamToBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (stream != null) {
            byte[] buf = new byte[1024];
            int r;
            while ((r = stream.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
        }
        return baos.toByteArray();
    }

    @Override
    protected void onSetupUI() {
        if (isPortrait()) {
            mMjpegView.setThumbnailScale(3);
            mMjpegView.setBackgroundColor(getResources().getColor(R.color.previewBackground));
        } else {
            mMjpegView.setThumbnailScale(4);
            mMjpegView.setBackgroundColor(Color.BLACK);
        }
        updateRecordState();
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
        CameraState states = VdtCamera.getCameraStates(mVdtCamera);
        if (states.mRecordState == CameraState.State_Record_Recording) {
            mVdtCamera.getClient().ack_Cam_stop_rec();
        } else if (states.mRecordState == CameraState.State_Record_Stopped) {
            if (!states.mbIsStill) {
                mVdtCamera.getClient().cmd_Cam_start_rec();
            } else {
                switch (mStillCaptureState) {
                    case STILLCAP_STATE_IDLE:
                        break;
                    case STILLCAP_STATE_WAITING:
                        mStillCaptureState = STILLCAP_STATE_IDLE;
                        mVdtCamera.getClient().cmd_Rec_StartStillCapture(true);
                        //mMjpegView.startMask(5);
                        //BeepManager.play(thisApp, R.raw.beep2, false);
                        mStillCaptureTimer.cancel();
                        break;
                    case STILLCAP_STATE_BURSTING:
                        mVdtCamera.getClient().cmd_Rec_StopStillCapture();
                        mStillCaptureState = STILLCAP_STATE_IDLE;
                        break;
                }
            }
        }
    }

    // set a timer to do still capture
    private void onRecordButtonPressed() {
        CameraState states = VdtCamera.getCameraStates(mVdtCamera);
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
                mVdtCamera.getClient().cmd_Rec_StartStillCapture(false);
            }
            mStillCaptureState = STILLCAP_STATE_BURSTING;
        }
    }

    private void updateRecordTime() {
        CameraState states = VdtCamera.getCameraStates(mVdtCamera);
        if (states.mRecordState == CameraState.State_Record_Recording) {
            if (mUpdateTimer.tag == TIMER_IDLE) {
                states.mRecordDuration = -2; // TODO
                states.mbRecordDurationUpdated = false;
                mVdtCamera.getClient().ack_Cam_get_time();
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
        CameraState states = VdtCamera.getCameraStates(mVdtCamera);
        boolean bEnable = true;
        switch (states.mRecordState) {
            default:
            case CameraState.State_Record_Unknown:
                mRecordButton.setVisibility(View.GONE);
                bEnable = false;
                break;
            case CameraState.State_Record_Stopped:
                mRecordButton.changeImages(R.drawable.start_record, R.drawable.start_record_pressed);
                mRecordButton.setVisibility(visibility);
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
        CameraState states = VdtCamera.getCameraStates(mVdtCamera);
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
        CameraState states = VdtCamera.getCameraStates(mVdtCamera);

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
        CameraState states = VdtCamera.getCameraStates(mVdtCamera);
        if (states.mRecordState == CameraState.State_Record_Stopped) {
            mVdtCamera.getClient().cmd_Rec_SetStillMode(false);
            updateModeState();
        }
    }

    private void onClickPictureMode() {
        CameraState states = VdtCamera.getCameraStates(mVdtCamera);
        if (states.mRecordState == CameraState.State_Record_Stopped) {
            mVdtCamera.getClient().cmd_Rec_SetStillMode(true);
            updateModeState();
        }
    }


    /*
    private final VdtCamera.Callback mCameraCallback = new VdtCamera.Callback() {

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

        @Override
        public void onStartRecordError(VdtCamera vdtCamera, int error) {
            if (vdtCamera == mVdtCamera) {
                CameraControlActivity.this.onStartRecordError(error);
            }
        }

        @Override
        public void onHostSSIDFetched(VdtCamera vdtCamera, String ssid) {

        }

        @Override
        public void onScanBtDone(VdtCamera vdtCamera) {

        }

        @Override
        public void onBtDevInfo(VdtCamera vdtCamera, int type, String mac, String name) {

        }

        @Override
        public void onConnected(VdtCamera vdtCamera) {

        }

        @Override
        public void onDisconnected(VdtCamera vdtCamera) {
            if (vdtCamera == mVdtCamera) {
                removeCamera();
                noCamera();
            }
        }

        @Override
        public void onStillCaptureStarted(VdtCamera vdtCamera, boolean bOneShot) {
            if (vdtCamera == mVdtCamera && bOneShot) {
                mMjpegView.startMask(5);
                BeepManager.play(thisApp, R.raw.beep2, false);
            }
        }

        @Override
        public void onStillPictureInfo(VdtCamera vdtCamera, boolean bCapturing, int numPictures, int burstTicks) {
            if (vdtCamera == mVdtCamera) {
                if (bCapturing) {
                    startBurst(numPictures, burstTicks);
                } else {
                    stopBurst();
                }
            }
        }

        @Override
        public void onStillCaptureDone(VdtCamera vdtCamera) {
            // TODO
        }

    }; */

    class MyMjpegViewCallback implements MjpegBitmap.Callback {

        @Override
        public void onDown() {
            // toggleToolbar();
        }

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
