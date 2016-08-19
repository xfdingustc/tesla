package com.waylens.hachi.ui.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.library.crs_svr.HashUtils;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.liveview.LiveViewSettingActivity;
import com.waylens.hachi.ui.services.download.InetDownloadService;
import com.xfdingustc.snipe.SnipeError;
import com.xfdingustc.snipe.VdbResponse;
import com.xfdingustc.snipe.toolbox.GetSpaceInfoRequest;
import com.xfdingustc.snipe.vdb.SpaceInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.w3c.dom.Text;

import java.io.File;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class CameraSettingFragment extends PreferenceFragment {
    private static final String TAG = CameraSettingFragment.class.getSimpleName();
    private static final String DOWNLOAD_FOLDER_NAME = "Waylens";
    private static final String DOWNLOAD_FILE_NAME = "firmware";
    private static final String[] AUTO_OFF_TIME = {"Never", "10s", "30s", "60s", "2min", "5min"};

    private static final String[] POWER_AUTO_OFF_TIME = {"Never", "30s", "60s", "2min", "5min"};

    private static final String[] SCREEN_SAVER_STYLE = {"All Black", "Dot"};
    private VdtCamera mVdtCamera;

    private Preference mCameraName;
    private Preference mVideo;
    private Preference mAudio;
    private Preference mBookmark;
    private Preference mStorage;
    private Preference mConnectivity;
    private Preference mFirmware;
    private Preference mDisplay;
    private Preference mPower;


    private FirmwareInfo mFirmwareInfo;


    private NumberPicker mBeforeNumber;
    private NumberPicker mAfterNumber;

    private Switch mMicSwitch;
    private Switch mSpeakerSwitch;
    private SeekBar mAudioSeekbar;
    private ImageView mSpeakerImage;

    private SeekBar mBrightnessSeekbar;
    private NumberPicker mAutoOffNumber;
    private TextView mBrightness;

    private TextView mTvPower;
    private NumberPicker mNpScreenSaver;
    private NumberPicker mNpAutoPowerOff;



    private PieChart mStorageChart;


    private static final int MAX_BOOKMARK_LENGHT = 30;


    private RequestQueue mRequestQueue;


    private MaterialDialog mDownloadProgressDialog;

    private MaterialDialog mUploadProgressDialog;

    private BroadcastReceiver mDownloadProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDownloadProgressDialog != null && mDownloadProgressDialog.isShowing()) {
                int what = intent.getIntExtra(InetDownloadService.EVENT_EXTRA_WHAT, -1);
                switch (what) {
                    case InetDownloadService.EVENT_WHAT_DOWNLOAD_PROGRESS:
                        int progress = intent.getIntExtra(InetDownloadService.EVENT_EXTRA_DOWNLOAD_PROGRESS, 0);
                        mDownloadProgressDialog.setProgress(progress);
                        break;
                    case InetDownloadService.EVENT_WHAT_DOWNLOAD_FINSHED:

                        mDownloadProgressDialog.setContent(R.string.download_complete);

                        final String file = intent.getStringExtra(InetDownloadService.EVENT_EXTRA_DOWNLOAD_FILE_PATH);
                        startFirmwareMd5Check(new File(file));

                }


            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_camera_setting);
        mVdtCamera = VdtCameraManager.getManager().getCurrentCamera();
        mRequestQueue = Volley.newRequestQueue(getActivity());
        mRequestQueue.start();
        initPreference();
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(InetDownloadService.INTENT_FILTER_DOWNLOAD_INTENT_SERVICE);
        getActivity().registerReceiver(mDownloadProgressReceiver, intentFilter);

    }


    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mDownloadProgressReceiver);
    }

    private void initPreference() {
        initCameraNamePreference();
        initBookmarkPreference();
        initVideoPreference();
        initAudioPreference();
        initStoragePreference();
        initDisplayPreference();
        initPowerPreference();
        initConnectivityPreference();
        initFirmwarePreference();
    }

    private void initFirmwarePreference() {
        mFirmware = findPreference("firmware");
        mFirmware.setSummary(mVdtCamera.getApiVersion());

        String url = Constants.API_CAMEAR_FIRMWARE;

        Request<JSONArray> request = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                Logger.t(TAG).json(response.toString());
                showFirmwareUpgradDialog(response);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Logger.t(TAG).e(error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> mHashMap = new HashMap<>();
                String token = SessionManager.getInstance().getToken();
                if (token != null && !token.isEmpty()) {
                    mHashMap.put("X-Auth-Token", token);
                }
                return mHashMap;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(1000 * 10, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Logger.t(TAG).d("fetch latest firmware");
        mRequestQueue.add(request);

    }

    private void showFirmwareUpgradDialog(JSONArray response) {
        for (int i = 0; i < response.length(); i++) {
            try {
                final FirmwareInfo firmwareInfo = FirmwareInfo.fromJson(response.getJSONObject(i));
                if (firmwareInfo.getName().equals(mVdtCamera.getHardwareName())) {
                    Logger.t(TAG).d("Found our hardware");
                    FirmwareVersion versionFromServer = new FirmwareVersion(firmwareInfo.getVersion());
                    FirmwareVersion versionInCamera = new FirmwareVersion(mVdtCamera.getApiVersion());
                    if (versionFromServer.isGreaterThan(versionInCamera)) {
                        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                            .title(R.string.found_new_firmware)
                            .content(firmwareInfo.getDescription())
                            .positiveText(R.string.upgrade)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    doDownloadFirmware(firmwareInfo);
                                }
                            })
                            .show();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }


    private void doDownloadFirmware(final FirmwareInfo firmwareInfo) {

        InetDownloadService.start(getActivity(), firmwareInfo.getUrl());

        mFirmwareInfo = firmwareInfo;

        showDownloadProgressDialog();


    }

    private void startFirmwareMd5Check(final File file) {
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                subscriber.onNext(0);
                final String downloadFileMd5 = HashUtils.MD5String(file);
                if (downloadFileMd5.equals(mFirmwareInfo.getMd5())) {
                    subscriber.onNext(1);
                } else {
                    subscriber.onNext(-1);
                }
            }

        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<Integer>() {

                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Integer integer) {
                    switch (integer) {
                        case 0:
                            mDownloadProgressDialog.setContent(R.string.firmware_check);
                            break;
                        case 1:
                            mDownloadProgressDialog.setContent(R.string.firmware_correct);
                            mDownloadProgressDialog.dismiss();
                            doSendFirmware2Camera(file);
                            break;
                        case -1:
                            mDownloadProgressDialog.setContent(R.string.firmware_corrupt);
                            break;
                    }

                }
            });


    }

    private void doSendFirmware2Camera(final File file) {
        mVdtCamera.sendNewFirmware(mFirmwareInfo.getMd5(), new VdtCamera.OnNewFwVersionListern() {
            @Override
            public void onNewVersion(int response) {
                Logger.t(TAG).d("response: " + response);
                if (response == 1) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mUploadProgressDialog = new MaterialDialog.Builder(getActivity())
                                .content(R.string.upload_firmware)
                                .progress(false, (int) file.length(), false)
                                .contentGravity(GravityEnum.CENTER)
                                .cancelable(false)
                                .show();
                        }
                    });

                    Observable.create(new Observable.OnSubscribe<Integer>() {
                        @Override
                        public void call(Subscriber<? super Integer> subscriber) {
                            FirmwareWriter writer = new FirmwareWriter(file, mVdtCamera, subscriber);
                            writer.start();
                            subscriber.onCompleted();

                        }
                    })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Integer>() {

                            @Override
                            public void onCompleted() {
                                MainActivity.launch(getActivity());
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onNext(Integer integer) {
                                mUploadProgressDialog.setProgress(integer);
                            }
                        });


                } else if (response == 0) {
                    doUpgradeFirmware();
                }
            }
        });

    }

    private void doUpgradeFirmware() {

        mVdtCamera.upgradeFirmware();
    }


    private void showDownloadProgressDialog() {
        mDownloadProgressDialog = new MaterialDialog.Builder(getActivity())
            .content(R.string.download_firmware)
            .progress(false, 100, false)
            .contentGravity(GravityEnum.CENTER)
            .cancelable(false)
            .show();
    }

    private void initConnectivityPreference() {
        mConnectivity = findPreference("connectivity");
        mConnectivity.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ConnectivitySettingActivity.launch(getActivity());
                return true;
            }
        });
    }

    private void initStoragePreference() {
        mStorage = findPreference("storage");
        mStorage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showStorageInfo();
                return true;
            }
        });
    }

    private void initDisplayPreference() {
        mDisplay = findPreference("display");
        mDisplay.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.display)
                        .customView(R.layout.dialog_display_setting, true)
                        .positiveText(R.string.ok)
                        .negativeText(R.string.cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                int brightness = mBrightnessSeekbar.getProgress();
                                int autoOffTimePos = mAutoOffNumber.getValue();
                                String autoOffTime = AUTO_OFF_TIME[autoOffTimePos];
/*                                int screenSaverPos = mNpScreenSaver.getValue();
                                String screenSaver = SCREEN_SAVER_STYLE[screenSaverPos];*/
                                mVdtCamera.setDisplayBrightness(brightness);
                                mVdtCamera.setDisplayAutoOffTime(autoOffTime);
/*                                mVdtCamera.setScreenSaverStyle(screenSaver);
                                mVdtCamera.getScreenSaverStyle();*/
                            }
                        })
                        .show();

                mBrightnessSeekbar = (SeekBar) dialog.getCustomView().findViewById(R.id.sbBrightness);
                mBrightness = (TextView) dialog.getCustomView().findViewById(R.id.tv_brightness);
                mAutoOffNumber = (NumberPicker) dialog.getCustomView().findViewById(R.id.npAutoOff);
/*                mNpScreenSaver = (NumberPicker) dialog.getCustomView().findViewById(R.id.npScreen);
                mNpScreenSaver.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);*/

/*                Field[] pickerFields = NumberPicker.class.getDeclaredFields();
                for (Field pf : pickerFields) {
                    if (pf.getName().equals("mSelectionDividersDistance")) {
                        pf.setAccessible(true);
                        try {
                            int result = 72;
                            pf.set(mAutoOffNumber, result);
                        } catch (IllegalAccessException e) {
                            Logger.t(TAG).d(e.getMessage());
                        }
                    }
                }*/

                mBrightnessSeekbar.setMax(10);
                mAutoOffNumber.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                mAutoOffNumber.setDisplayedValues(AUTO_OFF_TIME);
                mAutoOffNumber.setMinValue(0);
                mAutoOffNumber.setMaxValue(AUTO_OFF_TIME.length - 1);

/*                mNpScreenSaver.setDisplayedValues(SCREEN_SAVER_STYLE);
                mNpScreenSaver.setMinValue(0);
                mNpScreenSaver.setMaxValue(SCREEN_SAVER_STYLE.length - 1);

                String screenSaverStyle = mVdtCamera.getScreenSaverStyle();
                int screenSaverStylePos = -1;
                for(int i = 0; i < SCREEN_SAVER_STYLE.length; i++) {
                    if (SCREEN_SAVER_STYLE[i].equals(screenSaverStyle)) {
                        screenSaverStylePos = i;
                        break;
                    }
                }
                if (screenSaverStylePos != -1) {
                    mNpScreenSaver.setValue(screenSaverStylePos);
                }*/

                int brightness = mVdtCamera.getDisplayBrightness();
                String autoOffTime = mVdtCamera.getDisplayAutoOffTime();
                int autoOffTimePos = -1;
                for(int i = 0; i < AUTO_OFF_TIME.length; i++) {
                    if (AUTO_OFF_TIME[i].equals(autoOffTime)) {
                        autoOffTimePos = i;
                        break;
                    }
                }
                mBrightnessSeekbar.setProgress(brightness);
                mBrightness.setText(String.valueOf(brightness));
                if (autoOffTimePos != 0) {
                    mAutoOffNumber.setValue(autoOffTimePos);
                }

                mBrightnessSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        Logger.t(TAG).d(i);
                        mBrightness.setText(String.valueOf(i));
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

                return true;
            }
        });
    }

    private void initPowerPreference() {
        mPower = findPreference("power");
        mPower.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.power)
                        .customView(R.layout.dialog_power_setting, true)
                        .positiveText(R.string.ok)
                        .negativeText(R.string.cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                int autoOffTimePos = mNpAutoPowerOff.getValue();
                                String autoOffTime = POWER_AUTO_OFF_TIME[autoOffTimePos];
                                mVdtCamera.setAutoPowerOffDelay(autoOffTime);
                            }
                        })
                        .show();

                mTvPower = (TextView) dialog.getCustomView().findViewById(R.id.tv_power);
                mNpAutoPowerOff  = (NumberPicker) dialog.getCustomView().findViewById(R.id.npAutoOff);

/*                Field[] pickerFields = NumberPicker.class.getDeclaredFields();
                for (Field pf : pickerFields) {
                    if (pf.getName().equals("mSelectionDividersDistance")) {
                        pf.setAccessible(true);
                        try {
                            int result = 72;
                            pf.set(mNpAutoPowerOff, result);
                        } catch (IllegalAccessException e) {
                            Logger.t(TAG).d(e.getMessage());
                        }
                    }
                }*/

                mNpAutoPowerOff.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

                int batteryVolume = mVdtCamera.getBatteryVolume();
                mTvPower.setText(String.valueOf(batteryVolume));

                mNpAutoPowerOff.setDisplayedValues(POWER_AUTO_OFF_TIME);
                mNpAutoPowerOff.setMinValue(0);
                mNpAutoPowerOff.setMaxValue(POWER_AUTO_OFF_TIME.length - 1);

                String autoPowerOffDelay = mVdtCamera.getAutoPowerOffDelay();
                int autoPowerOffDelayPos = -1;
                for(int i = 0; i < POWER_AUTO_OFF_TIME.length; i++) {
                    if (POWER_AUTO_OFF_TIME[i].equals(autoPowerOffDelay)) {
                        autoPowerOffDelayPos = i;
                        break;
                    }
                }
                if (autoPowerOffDelayPos != -1) {
                    mNpAutoPowerOff.setValue(autoPowerOffDelayPos);
                }
                return true;
            }
        });
    }

    private void showStorageInfo() {
        GetSpaceInfoRequest request = new GetSpaceInfoRequest(new VdbResponse.Listener<SpaceInfo>() {
            @Override
            public void onResponse(SpaceInfo response) {
                showStorageChart(response);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });

        mVdtCamera.getRequestQueue().add(request);
    }

    private void showStorageChart(SpaceInfo response) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
            .customView(R.layout.dialog_storage_info, true)
            .show();

        mStorageChart = (PieChart) dialog.getCustomView().findViewById(R.id.pieChart);


        List<String> storageName = Arrays.asList("Marked", "Used", "Free");
        List<Entry> spaces = new ArrayList<>();
        spaces.add(new Entry(response.marked / (1024 * 1024), 1));
        spaces.add(new Entry((response.used - response.marked) / (1024 * 1024), 2));
        spaces.add(new Entry((response.total - response.used) / (1024 * 1024), 3));

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(getResources().getColor(R.color.style_color_primary));
        colors.add(getResources().getColor(R.color.material_deep_orange_300));
        colors.add(Color.BLACK);

        PieDataSet dataSet = new PieDataSet(spaces, "Storage Info");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(colors);

        PieData data = new PieData(storageName, dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                if (value > 1024) {
                    BigDecimal tmp = new BigDecimal(value / 1024);
                    return String.valueOf(tmp.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue()) + " G";
                } else {
                    BigDecimal tmp = new BigDecimal(value);
                    return String.valueOf(tmp.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue()) + " M";
                }
            }
        });
        data.setValueTextSize(8f);
        data.setValueTextColor(Color.WHITE);

        mStorageChart.setData(data);
        mStorageChart.setCenterText("Storage Info");
        mStorageChart.highlightValue(null);
        mStorageChart.invalidate();
    }

    private void initAudioPreference() {
        mAudio = findPreference("audio");
        mAudio.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.audio_setting)
                    .customView(R.layout.dialog_audio_setting, true)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            mVdtCamera.setAudioMic(mMicSwitch.isChecked(), 0);
                            int volume = mAudioSeekbar.getProgress();
                            mVdtCamera.setSpeakerStatus(mSpeakerSwitch.isChecked(), volume);
                        }
                    })
                    .show();

                mMicSwitch = (Switch) dialog.getCustomView().findViewById(R.id.swMic);
                mAudioSeekbar = (SeekBar) dialog.getCustomView().findViewById(R.id.sbSpeakerVolume);
                mSpeakerSwitch = (Switch) dialog.getCustomView().findViewById(R.id.swSpeaker);
                mSpeakerImage = (ImageView) dialog.getCustomView().findViewById(R.id.speakerImage);
                mAudioSeekbar.setMax(10);

                boolean isMicOn = mVdtCamera.isMicOn();
                boolean isSpeakerOn = mVdtCamera.isSpeakerOn();
                int speakerVol = mVdtCamera.getSpeakerVol();
                mSpeakerSwitch.setChecked(isSpeakerOn);
                if (!isSpeakerOn) {
                    mAudioSeekbar.setVisibility(View.INVISIBLE);
                    mSpeakerImage.setVisibility(View.INVISIBLE);
                }
                mAudioSeekbar.setProgress(speakerVol);
                mMicSwitch.setChecked(isMicOn);
                mSpeakerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        mAudioSeekbar.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
                        mSpeakerImage.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
                    }
                });

                return true;
            }
        });
    }

    private void initVideoPreference() {
        mVideo = findPreference("video");
        mVideo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LiveViewSettingActivity.launch(getActivity());
                return true;
            }
        });

    }


    private void initCameraNamePreference() {
        mCameraName = findPreference("cameraName");
        mCameraName.setSummary(mVdtCamera.getName());

        mCameraName.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.camera_name)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .input(getString(R.string.camera_name), mVdtCamera.getName(), new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                        }
                    })
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            String nameCameraName = dialog.getInputEditText().getText().toString();
                            mCameraName.setSummary(nameCameraName);
                            mVdtCamera.setName(nameCameraName);
                        }
                    }).show();
                return true;
            }
        });
    }

    private void initBookmarkPreference() {
        mBookmark = findPreference("bookmark");
        mBookmark.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.bookmark)
                        .customView(R.layout.dialog_bookmark_change, true)
                        .positiveText(R.string.ok)
                        .negativeText(R.string.cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                mVdtCamera.setMarkTime(mBeforeNumber.getValue(), mAfterNumber.getValue());
                            }
                        }).show();

                mBeforeNumber = (NumberPicker) dialog.getCustomView().findViewById(R.id.npBefore);
                mAfterNumber = (NumberPicker) dialog.getCustomView().findViewById(R.id.npAfter);
                mBeforeNumber.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                mAfterNumber.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                mBeforeNumber.setMaxValue(MAX_BOOKMARK_LENGHT);
                mAfterNumber.setMaxValue(MAX_BOOKMARK_LENGHT);
                mBeforeNumber.setMinValue(0);
                mAfterNumber.setMinValue(0);
                mBeforeNumber.setValue(mVdtCamera.getMarkBeforeTime());
                mAfterNumber.setValue(mVdtCamera.getMarkAfterTime());


                mBeforeNumber.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        int afterNumber = mAfterNumber.getValue();
                        afterNumber = Math.min(afterNumber, MAX_BOOKMARK_LENGHT - newVal);
                        mAfterNumber.setValue(afterNumber);
                    }
                });

                mAfterNumber.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        int beforeNumber = mBeforeNumber.getValue();
                        beforeNumber = Math.min(beforeNumber, MAX_BOOKMARK_LENGHT - newVal);
                        mBeforeNumber.setValue(beforeNumber);
                    }
                });

                return true;

            }
        });
    }


    private static class FirmwareVersion {
        private int mMain;
        private int mSub;
        private int mBuild;

        public FirmwareVersion(String firmware) {
            int main = 0, sub = 0;
            String build = "";
            int i_main = firmware.indexOf('.', 0);
            if (i_main >= 0) {
                String t = firmware.substring(0, i_main);
                main = Integer.parseInt(t);
                i_main++;
                int i_sub = firmware.indexOf('.', i_main);
                if (i_sub >= 0) {
                    t = firmware.substring(i_main, i_sub);
                    sub = Integer.parseInt(t);
                    i_sub++;
                    build = firmware.substring(i_sub);
                }
            }
            mMain = main;
            mSub = sub;
            mBuild = Integer.parseInt(build);
        }


        public boolean isGreaterThan(FirmwareVersion firmwareVersion) {
            if (this.mMain > firmwareVersion.mMain) {
                return true;
            }
            if (this.mSub > firmwareVersion.mSub) {
                return true;
            }

            if (this.mBuild > firmwareVersion.mBuild) {
                return true;
            }

            return false;
        }
    }


}
