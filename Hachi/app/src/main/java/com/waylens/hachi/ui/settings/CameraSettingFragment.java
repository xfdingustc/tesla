package com.waylens.hachi.ui.settings;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.NumberPicker;
import android.widget.SeekBar;

import com.afollestad.materialdialogs.DialogAction;
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
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.preference.seekbarpreference.SeekBarPreference;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.utils.Utils;
import com.xfdingustc.snipe.SnipeError;
import com.xfdingustc.snipe.VdbResponse;

import com.xfdingustc.snipe.toolbox.GetSpaceInfoRequest;
import com.xfdingustc.snipe.vdb.SpaceInfo;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;


public class CameraSettingFragment extends PreferenceFragment {
    private static final String TAG = CameraSettingFragment.class.getSimpleName();

    private VdtCamera mVdtCamera;
    private Preference mCameraName;
    private ListPreference mResolution;
    private SwitchPreference mTimestamp;
    private SwitchPreference mMic;
    private SwitchPreference mSpeaker;
    private SeekBarPreference mSpeakerVol;
    private Preference mBookmark;
    private Preference mStorage;
    private Preference mBluetooth;
    private Preference mFirmware;
    private Preference mWifi;

    private Preference mSyncTimezone;
    private SeekBarPreference mBrightness;
    private ListPreference mScreenSaver;
    private ListPreference mScreenSaverStyle;
    //    private Preference mDisplay;
    private Preference mBattery;
    private ListPreference mAutoPowerOffTime;


    private NumberPicker mBeforeNumber;
    private NumberPicker mAfterNumber;

    private static final int MAX_BOOKMARK_LENGHT = 30;


    private RequestQueue mRequestQueue;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_camera_setting);
        mVdtCamera = VdtCameraManager.getManager().getCurrentCamera();
        mRequestQueue = Volley.newRequestQueue(getActivity());
        mRequestQueue.start();
        initPreference();
    }


    private void initPreference() {
        mCameraName = findPreference("cameraName");
        mBookmark = findPreference("bookmark");
        mResolution = (ListPreference) findPreference("resolution");
        mTimestamp = (SwitchPreference) findPreference("timestamp");
        mMic = (SwitchPreference) findPreference("mic");
        mSpeaker = (SwitchPreference) findPreference("speaker");
        mSpeakerVol = (SeekBarPreference) findPreference("speakerVol");
        mBrightness = (SeekBarPreference) findPreference("brightness");
        mStorage = findPreference("storage");
        mScreenSaverStyle = (ListPreference) findPreference("screen_saver_style");
        mScreenSaver = (ListPreference) findPreference("screen_saver");
        mBattery = findPreference("battery");
        mBluetooth = findPreference("bluetooth");
        mFirmware = findPreference("firmware");
        mSyncTimezone = findPreference("sync_timezone");
        mWifi = findPreference("wifi");
        initCameraNamePreference();
        initBookmarkPreference();
        initVideoPreference();
        initAudioPreference();
        initDisplayPreference();
        initPowerPreference();
        initConnectivityPreference();
        initFirmwarePreference();
        initSyncTimezonePreference();
        initWifiPreference();
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
                Logger.t(TAG).d(response.getJSONObject(i).toString());
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
                                    FirmwareUpdateActivity.launch(getActivity(), firmwareInfo);
                                }
                            })
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    mFirmware.setSummary(mFirmware.getSummary() + " (" + getString(R.string.found_new_firmware) + ")");
                                    mFirmware.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                        @Override
                                        public boolean onPreferenceClick(Preference preference) {
                                            FirmwareUpdateActivity.launch(getActivity(), firmwareInfo);
                                            return true;
                                        }
                                    });
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


    private void initConnectivityPreference() {
        mBluetooth.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                BluetoothSettingActivity.launch(getActivity());
                return true;
            }
        });

    }

    private void initSyncTimezonePreference() {
        mSyncTimezone.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mVdtCamera.syncTimezone();
                Snackbar.make(CameraSettingFragment.this.getView(), "Synchronize timezone successfully!", Snackbar.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void initWifiPreference() {
        mWifi.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                WifiSettingActivity.launch(getActivity());
                return true;
            }
        });
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
        mScreenSaver.setValue(mVdtCamera.getScreenSaverTime());
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
        mScreenSaverStyle.setValue(mVdtCamera.getScreenSaverStyle());
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
        if (!TextUtils.isEmpty(screenSave) && screenSave.equals("Never")) {
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
        mAutoPowerOffTime.setValue(mVdtCamera.getAutoPowerOffDelay());
        mAutoPowerOffTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Logger.t(TAG).d("new Value: " + o.toString());
                mAutoPowerOffTime.setSummary(o.toString());
                mVdtCamera.setAutoPowerOffDelay(o.toString());
                mAutoPowerOffTime.setValue(mVdtCamera.getAutoPowerOffDelay());
                return false;
            }
        });


    }


    private void initAudioPreference() {

        mMic.setChecked(mVdtCamera.isMicEnabled());
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

        mResolution.setSummary(mVdtCamera.getVideoResolutionStr());
        Logger.t(TAG).d("video resolution: " + mVdtCamera.getVideoResolutionFramerate());
        mResolution.setValue(String.valueOf(mVdtCamera.getVideoResolutionFramerate()));
        mResolution.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                mVdtCamera.setVideoResolution(Integer.valueOf(o.toString()));
                mResolution.setSummary(mVdtCamera.getVideoResolutionStr());
//                mVdtCamera.setVideoResolution(mResolution.getValue());
                return true;
            }
        });

        boolean isOverlayShown = (mVdtCamera.getOverlayState() == 0) ? false : true;
        mTimestamp.setChecked(isOverlayShown);
        mTimestamp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                int newOverlayState = mTimestamp.isChecked() ? 0 : 2;
                Logger.t(TAG).d("new overlay state: " + newOverlayState);
                mVdtCamera.setOverlayState(newOverlayState);
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
