package com.waylens.hachi.ui.settings;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.util.Log;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Switch;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.bigkoo.pickerview.OptionsPickerView;
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
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.GetSpaceInfoRequest;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.vdb.SpaceInfo;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Xiaofei on 2016/5/3.
 */
public class CameraSettingFragment extends PreferenceFragment {
    private static final String TAG = CameraSettingFragment.class.getSimpleName();
    private static final String DOWNLOAD_FOLDER_NAME = "Waylens";
    private static final String DOWNLOAD_FILE_NAME = "firmware";
    private VdtCamera mVdtCamera;

    private Preference mCameraName;
    private Preference mVideo;
    private Preference mAudio;
    private Preference mBookmark;
    private Preference mStorage;
    private Preference mConnectivity;
    private Preference mFirmware;


    private NumberPicker mBeforeNumber;
    private NumberPicker mAfterNumber;

    private Switch mMicSwitch;
    private Switch mSpeakerSwitch;
    private SeekBar mAudioSeekbar;

    private PieChart mStorageChart;

    private static final int MAX_BOOKMARK_LENGHT = 30;

    private OptionsPickerView mQualityPickerView;
    private ArrayList<String> mResolutionList = new ArrayList<>();
    private ArrayList<ArrayList<String>> mFrameRateList = new ArrayList<>();

    private int mChangedVideoResolution;
    private int mChangedVideoFramerate;

    private RequestQueue mRequestQueue;
    private DownloadManager mDownloadManager;
    private long mDownloadId;

    private MaterialDialog mDownloadProgressDialog;
//    private ContentObserver mDownloadObserver;
    private DownloadChangeObserver mDownloadObserver;
    private String mDownloadedFile;


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    if (mDownloadProgressDialog == null) {
                        return;
                    }

                    int arg1 = msg.arg1 / 1024;
                    int arg2 = msg.arg2 / 1024;

                    mDownloadProgressDialog.setMaxProgress(arg2);

                    mDownloadProgressDialog.setProgress(arg1);

                    if (arg1 >= arg2) {
                        mDownloadProgressDialog.setContent(R.string.download_complete);
                    }

                    break;

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
    public void onResume() {
        super.onResume();
        mDownloadObserver = new DownloadChangeObserver(mHandler);
        getActivity().getContentResolver().registerContentObserver(Uri.parse("content://downloads/my_downloads"),
            true, mDownloadObserver);
    }



    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(mDownloadObserver);
    }




    private void initPreference() {
        initCameraNamePreference();
        initBookmarkPreference();
        initVideoPreference();
        initAudioPreference();
        initStoragePreference();
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
                                    .positiveText(R.string.upgrade)
                                    .negativeText(android.R.string.cancel)
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
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Logger.t(TAG).e(error.toString());
            }
        });
        request.setRetryPolicy(new DefaultRetryPolicy(1000 * 10, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mRequestQueue.add(request);

    }

    private void doDownloadFirmware(FirmwareInfo firmwareInfo) {
        File folder = Environment.getExternalStoragePublicDirectory(DOWNLOAD_FOLDER_NAME);
        if (!folder.exists() || !folder.isDirectory()) {
            folder.mkdirs();
        }
        mDownloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(firmwareInfo.getUrl());
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDestinationInExternalPublicDir(DOWNLOAD_FOLDER_NAME, DOWNLOAD_FILE_NAME);
        mDownloadId = mDownloadManager.enqueue(request);
//        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
//        request.setAllowedOverRoaming(true);
//        request.setMimeType("application/vnd.android.package-archive");
//        request.setVisibleInDownloadsUi(true);
//        String url = firmwareInfo.getUrl();
//
//        String firmwareFile = url.substring(url.lastIndexOf("/") + 1);
//        request.setDestinationInExternalPublicDir("/download/", firmwareFile);
//        request.setTitle(getString(R.string.app_name) + firmwareFile);
//        mDownloadId = mDownloadManager.enqueue(request);
        showDownloadProgressDialog();
    }


    private void showDownloadProgressDialog() {
        mDownloadProgressDialog = new MaterialDialog.Builder(getActivity())
            .title(R.string.downloading)
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
        List<Entry> spaces = new ArrayList<Entry>();
        spaces.add(new Entry(response.marked / (1024 * 1024), 1));
        spaces.add(new Entry((response.used - response.marked) / (1024 * 1024), 2));
        spaces.add(new Entry((response.total - response.used) / (1024 * 1024), 3));

        ArrayList<Integer> colors = new ArrayList<Integer>();
        colors.add(getResources().getColor(R.color.style_color_accent));
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
                return String.valueOf(value) + " M";
            }
        });
        data.setValueTextSize(11f);
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
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            mVdtCamera.setAudioMic(mMicSwitch.isChecked(), 0);

                        }
                    })
                    .show();

                mMicSwitch = (Switch) dialog.getCustomView().findViewById(R.id.swMic);
                mAudioSeekbar = (SeekBar) dialog.getCustomView().findViewById(R.id.sbMicVolume);
                mSpeakerSwitch = (Switch) dialog.getCustomView().findViewById(R.id.swSpeaker);

                boolean isMicOn = mVdtCamera.isMicOn();
                mMicSwitch.setChecked(isMicOn);

                return true;
            }
        });
    }

    private void initVideoPreference() {
        mVideo = findPreference("video");
        mVideo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
//                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
//                    .title(R.string.video_setting)
//                    .customView(R.layout.dialog_video_setting, true)
//                    .positiveText(android.R.string.ok)
//                    .negativeText(android.R.string.cancel)
//                    .onPositive(new MaterialDialog.SingleButtonCallback() {
//                        @Override
//                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//
//                        }
//                    })
//                    .show();

                mQualityPickerView.show();
                return true;
            }
        });
        initRecordQualityOptionPickerView();
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
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            mVdtCamera.setMarkTime(mBeforeNumber.getValue(), mAfterNumber.getValue());
                        }
                    }).show();

                mBeforeNumber = (NumberPicker) dialog.getCustomView().findViewById(R.id.npBefore);
                mAfterNumber = (NumberPicker) dialog.getCustomView().findViewById(R.id.npAfter);
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


    private void initRecordQualityOptionPickerView() {
        mQualityPickerView = new OptionsPickerView(getActivity());

        mResolutionList.add("1080P");
        mResolutionList.add("720P");

        ArrayList<String> framerateItem_01 = new ArrayList<>();
        framerateItem_01.add("30fps");
        framerateItem_01.add("60fps");
        framerateItem_01.add("120fps");

        ArrayList<String> framerateItem_02 = new ArrayList<>();
        framerateItem_02.add("30fps");
        framerateItem_02.add("60fps");
        framerateItem_02.add("120fps");

        mFrameRateList.add(framerateItem_01);
        mFrameRateList.add(framerateItem_02);

        mQualityPickerView.setPicker(mResolutionList, mFrameRateList, true);
        mQualityPickerView.setCyclic(false, false, false);

        mQualityPickerView.setOnoptionsSelectListener(new OptionsPickerView.OnOptionsSelectListener() {
            @Override
            public void onOptionsSelect(int options1, int option2, int options3) {
                if (options1 == 0) {
                    mChangedVideoResolution = VdtCamera.VIDEO_RESOLUTION_1080P;
                } else {
                    mChangedVideoResolution = VdtCamera.VIDEO_RESOLUTION_720P;
                }

                if (option2 == 0) {
                    mChangedVideoFramerate = VdtCamera.VIDEO_FRAMERATE_30FPS;
                } else if (option2 == 1) {
                    mChangedVideoFramerate = VdtCamera.VIDEO_FRAMERATE_60FPS;
                } else {
                    mChangedVideoFramerate = VdtCamera.VIDEO_FRAMERATE_120FPS;
                }

                showConfirmDialog();
            }


        });
    }

    private void showConfirmDialog() {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
            .title(R.string.change_camera_setting)
            .content(R.string.change_camera_setting_hint)
            .positiveText(android.R.string.ok)
            .negativeText(android.R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    mVdtCamera.setVideoResolution(mChangedVideoResolution, mChangedVideoFramerate);
                    mVdtCamera.stopRecording();

                }
            }).show();
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


    private class DownloadChangeObserver extends ContentObserver {

        public DownloadChangeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateProgressView();
        }
    }

    private void updateProgressView() {
        int[] bytesAndStatus = getBytesAndStatus(mDownloadId);

        mHandler.sendMessage(mHandler.obtainMessage(0, bytesAndStatus[0], bytesAndStatus[1],
            bytesAndStatus[2]));
    }

    public int[] getBytesAndStatus(long downloadId) {
        int[] bytesAndStatus = new int[]{-1, -1, 0};
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor c = null;
        try {
            c = mDownloadManager.query(query);
            if (c != null && c.moveToFirst()) {
                bytesAndStatus[0] = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                bytesAndStatus[1] = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                bytesAndStatus[2] = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return bytesAndStatus;
    }





}
