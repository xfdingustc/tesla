package com.waylens.hachi.ui.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.widget.NumberPicker;

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
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.library.crs_svr.HashUtils;
import com.waylens.hachi.preference.seekbarpreference.SeekBarPreference;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.liveview.LiveViewSettingActivity;
import com.waylens.hachi.ui.services.download.InetDownloadService;
import com.waylens.hachi.utils.Utils;
import com.xfdingustc.snipe.SnipeError;
import com.xfdingustc.snipe.VdbResponse;
import com.xfdingustc.snipe.toolbox.GetSpaceInfoRequest;
import com.xfdingustc.snipe.vdb.SpaceInfo;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.HashMap;
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

    private static final String POWER_AUTOOFF_NEVER = "Never";
    private static final String POWER_AUTOOFF_30S = "30s";

    private static final String[] POWER_AUTO_OFF_TIME = {"Never", "30s", "60s", "2min", "5min"};

    private static final String[] SCREEN_SAVER_STYLE = {"All Black", "Dot"};
    private VdtCamera mVdtCamera;

    private Preference mCameraName;
    private Preference mVideo;
    private SwitchPreference mMic;
    private SwitchPreference mSpeaker;
    private SeekBarPreference mSpeakerVol;
    private Preference mBookmark;
    private Preference mStorage;
    private Preference mConnectivity;
    private Preference mFirmware;
    private SeekBarPreference mBrightness;
    private ListPreference mScreenSaver;
    private ListPreference mScreenSaverStyle;
    //    private Preference mDisplay;
    private Preference mBattery;
    private ListPreference mAutoPowerOffTime;


    private FirmwareInfo mFirmwareInfo;


    private NumberPicker mBeforeNumber;
    private NumberPicker mAfterNumber;

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
        mCameraName = findPreference("cameraName");
        mBookmark = findPreference("bookmark");
        mVideo = findPreference("video");
        mMic = (SwitchPreference) findPreference("mic");
        mSpeaker = (SwitchPreference) findPreference("speaker");
        mSpeakerVol = (SeekBarPreference) findPreference("speakerVol");
        mBrightness = (SeekBarPreference) findPreference("brightness");
        mStorage = findPreference("storage");
        mScreenSaverStyle = (ListPreference) findPreference("screen_saver_style");
        mScreenSaver = (ListPreference) findPreference("screen_saver");
        mBattery = findPreference("battery");
        mConnectivity = findPreference("connectivity");
        mFirmware = findPreference("firmware");
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

        mConnectivity.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ConnectivitySettingActivity.launch(getActivity());
                return true;
            }
        });
    }

    private void initStoragePreference() {


    }

    private void initDisplayPreference() {

        Logger.t(TAG).d("display brightness" + mVdtCamera.getDisplayBrightness());
        mBrightness.setCurrentValue(mVdtCamera.getDisplayBrightness());
        mBrightness.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                mVdtCamera.setDisplayBrightness(mBrightness.getCurrentValue());
                return true;
            }
        });


        mScreenSaver.setSummary(mVdtCamera.getScreenSaverTime());
        updateScreenSaverStyle();
        mScreenSaver.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Logger.t(TAG).d("screen saver: " + o.toString());
                mVdtCamera.setScreenSaver(o.toString());
                mScreenSaver.setSummary(o.toString());
                updateScreenSaverStyle();
                return true;
            }
        });


        mScreenSaverStyle.setSummary(mVdtCamera.getScreenSaverStyle());
        mScreenSaverStyle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                mVdtCamera.setScreenSaverStyle(o.toString());
                mScreenSaverStyle.setSummary(o.toString());
                return true;
            }
        });

    }


    private void updateScreenSaverStyle() {
        String screenSave = mVdtCamera.getScreenSaverTime();
        if (screenSave.equals("Never")) {
            mScreenSaverStyle.setEnabled(false);
        } else {
            mScreenSaverStyle.setEnabled(true);
        }
    }

    private void initPowerPreference() {

        mBattery.setSummary(String.valueOf(mVdtCamera.getBatteryVolume()) + "%");

        String autoPowerOffDelay = mVdtCamera.getAutoPowerOffDelay();

        Logger.t(TAG).d("audio power off: " + autoPowerOffDelay);
        mAutoPowerOffTime = (ListPreference) findPreference("auto_power_off");
        mAutoPowerOffTime.setSummary(autoPowerOffDelay);
        mAutoPowerOffTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Logger.t(TAG).d("new Value: " + o.toString());
                mAutoPowerOffTime.setSummary(o.toString());
                mVdtCamera.setAutoPowerOffDelay(o.toString());
                return false;
            }
        });


    }


    private void initAudioPreference() {

        mMic.setChecked(mVdtCamera.isMicOn());
        mMic.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Logger.t(TAG).d("set audio mic: " + !mMic.isChecked());
                mVdtCamera.setAudioMic(!mMic.isChecked(), 0);
                return true;
            }
        });


        mSpeaker.setChecked(mVdtCamera.isSpeakerOn());
        mSpeaker.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                mVdtCamera.setSpeakerStatus(!mSpeaker.isChecked(), mVdtCamera.getSpeakerVol());
                mSpeakerVol.setEnabled(!mSpeaker.isChecked());
                return true;
            }
        });


        mSpeakerVol.setCurrentValue(mVdtCamera.getSpeakerVol());
        mSpeakerVol.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Logger.t(TAG).d("mSpeakerVol: " + mSpeakerVol.getCurrentValue());
                mVdtCamera.setSpeakerStatus(mSpeaker.isChecked(), mSpeakerVol.getCurrentValue());
                return true;
            }
        });
    }

    private void initVideoPreference() {

        mVideo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LiveViewSettingActivity.launch(getActivity());
                return true;
            }
        });

    }


    private void initCameraNamePreference() {

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
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
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



        mStorage.setSummary(R.string.calculating_space_info);
        mStorage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SpaceInfoActivity.launch(getActivity());
                return true;
            }
        });

        GetSpaceInfoRequest request = new GetSpaceInfoRequest(new VdbResponse.Listener<SpaceInfo>() {
            @Override
            public void onResponse(SpaceInfo response) {
                String leftSpace = Utils.getSpaceString(response.total - response.used);
                mStorage.setSummary(getString(R.string.space_free, leftSpace));
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });

        mVdtCamera.getRequestQueue().add(request);
    }

    private void initBookmarkPreference() {

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
