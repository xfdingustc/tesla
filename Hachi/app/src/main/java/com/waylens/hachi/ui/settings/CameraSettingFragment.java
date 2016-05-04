package com.waylens.hachi.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.widget.NumberPicker;


import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;

/**
 * Created by Xiaofei on 2016/5/3.
 */
public class CameraSettingFragment extends PreferenceFragment {
    private Preference mCameraName;

    private VdtCamera mVdtCamera;
    private Preference mBookmark;
    private SharedPreferences mSharedPrefs;

    private NumberPicker mBeforeNumber;
    private NumberPicker mAfterNumber;

    private static final int MAX_BOOKMARK_LENGHT = 30;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_camera_setting);
        mVdtCamera = VdtCameraManager.getManager().getCurrentCamera();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        initPreference();
    }

    private void initPreference() {
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


//        mTestText.

    }


}
