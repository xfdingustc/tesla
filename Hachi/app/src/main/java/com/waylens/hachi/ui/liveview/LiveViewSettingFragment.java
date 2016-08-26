package com.waylens.hachi.ui.liveview;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.waylens.hachi.R;
import com.xfdingustc.snipe.control.VdtCamera;
import com.xfdingustc.snipe.control.VdtCameraManager;


import org.greenrobot.eventbus.EventBus;

/**
 * Created by Xiaofei on 2016/5/29.
 */
public class LiveViewSettingFragment extends PreferenceFragment {
    private Preference mResolution;
    private Preference mFramerate;
    private Preference mTimestamp;
    private VdtCamera mVdtCamera;
    private int tmpResIndex;

    private EventBus mEventBus = EventBus.getDefault();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_live_view_setting);
        mVdtCamera = VdtCameraManager.getManager().getCurrentCamera();
        initPreference();
    }

    private void initPreference() {
        mResolution = findPreference("resolution");

        final int resolutionIndex = mVdtCamera.getVideoResolution();
        tmpResIndex = resolutionIndex;
        mResolution.setSummary(getResources().getStringArray(R.array.resolution_list)[resolutionIndex]);
        mResolution.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .items(R.array.resolution_list)
                        .itemsCallbackSingleChoice(resolutionIndex, new MaterialDialog.ListCallbackSingleChoice() {

                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                mResolution.setSummary(text);

                                mEventBus.post(new VideoSettingChangEvent(VideoSettingChangEvent.WHAT_RESOLUTION, which));
                                if (which == VdtCamera.VIDEO_RESOLUTION_1080P) {
                                    if (mVdtCamera.getVideoFramerate() == VdtCamera.VIDEO_FRAMERATE_120FPS) {
                                        mFramerate.setSummary(getResources().getStringArray(R.array.framerate_list)[1]);
                                        mEventBus.post(new VideoSettingChangEvent(VideoSettingChangEvent.WHAT_FRAMERATE, 1));

                                    }
                                }
                                tmpResIndex = which;
                                dialog.dismiss();
                                return false;
                            }
                        }).show();
                return true;
            }
        });


        mFramerate = findPreference("framerate");
        int framerateIndex = mVdtCamera.getVideoFramerate();
        mFramerate.setSummary(getResources().getStringArray(R.array.framerate_list)[framerateIndex]);
        mFramerate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .items(tmpResIndex == VdtCamera.VIDEO_RESOLUTION_720P ? R.array.framerate_list : R.array.framerate_1080_list)
                        .itemsCallbackSingleChoice(resolutionIndex, new MaterialDialog.ListCallbackSingleChoice() {

                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                mFramerate.setSummary(text);
                                dialog.dismiss();
                                mEventBus.post(new VideoSettingChangEvent(VideoSettingChangEvent.WHAT_FRAMERATE, which));
                                return false;
                            }
                        }).show();
                return true;
            }
        });

        mTimestamp = findPreference("timestamp");
        final int timeStampOn = mVdtCamera.getOverlayState() / 2;
        mTimestamp.setSummary(timeStampOn == 0 ? R.string.timestamp_off : R.string.timestamp_on);
        mTimestamp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .items(R.array.timestamp_state)
                        .itemsCallbackSingleChoice(timeStampOn, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                mTimestamp.setSummary(text);
                                dialog.dismiss();
                                mEventBus.post(new VideoSettingChangEvent(VideoSettingChangEvent.WHAT_TIMESTAMP, which * 2));
                                return true;
                            }
                        }).show();
                return true;
            }
        });
    }


}
