package com.waylens.hachi.ui.settings;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Switch;


import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bigkoo.pickerview.OptionsPickerView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.GetSpaceInfoRequest;
import com.waylens.hachi.vdb.SpaceInfo;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Xiaofei on 2016/5/3.
 */
public class CameraSettingFragment extends PreferenceFragment {
    private VdtCamera mVdtCamera;

    private Preference mCameraName;
    private Preference mVideo;
    private Preference mAudio;
    private Preference mBookmark;
    private Preference mStorage;


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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_camera_setting);
        mVdtCamera = VdtCameraManager.getManager().getCurrentCamera();
        initPreference();
    }

    private void initPreference() {
        initCameraNamePreference();
        initBookmarkPreference();
        initVideoPreference();
        initAudioPreference();
        initStoragePreference();
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

        mStorageChart = (PieChart)dialog.getCustomView().findViewById(R.id.pieChart);


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

                mMicSwitch = (Switch)dialog.getCustomView().findViewById(R.id.swMic);
                mAudioSeekbar = (SeekBar) dialog.getCustomView().findViewById(R.id.sbMicVolume);
                mSpeakerSwitch = (Switch)dialog.getCustomView().findViewById(R.id.swSpeaker);

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

                mBeforeNumber = (NumberPicker)dialog.getCustomView().findViewById(R.id.npBefore);
                mAfterNumber = (NumberPicker)dialog.getCustomView().findViewById(R.id.npAfter);
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

}
