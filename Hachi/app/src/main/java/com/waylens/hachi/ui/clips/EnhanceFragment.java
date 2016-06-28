package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.ViewAnimator;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.GaugeSettingManager;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.eventbus.events.GaugeEvent;
import com.waylens.hachi.ui.adapters.GaugeListAdapter;
import com.waylens.hachi.ui.clips.editor.clipseditview.ClipsEditView;
import com.waylens.hachi.ui.clips.player.ClipPlayFragment;
import com.waylens.hachi.ui.clips.player.GaugeInfoItem;
import com.waylens.hachi.ui.clips.player.PlaylistEditor;
import com.waylens.hachi.ui.clips.player.PlaylistUrlProvider;
import com.waylens.hachi.ui.entities.MusicItem;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;
import com.waylens.hachi.vdb.ClipSetPos;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;


public class EnhanceFragment extends BaseFragment {
    private static final String TAG = EnhanceFragment.class.getSimpleName();

    private static final int REQUEST_CODE_ENHANCE = 1000;
    private static final int REQUEST_CODE_ADD_MUSIC = 1001;

    public static final int DEFAULT_AUDIO_ID = -1;

    private static final int ACTION_NONE = -1;
    private static final int ACTION_OVERLAY = 0;
    private static final int ACTION_ADD_VIDEO = 1;
    private static final int ACTION_ADD_MUSIC = 2;
    private static final int ACTION_SMART_REMIX = 3;


    GaugeListAdapter mGaugeListAdapter;

    MusicItem mMusicItem;

    ArrayAdapter<CharSequence> mThemeAdapter;
    ArrayAdapter<CharSequence> mLengthAdapter;
    ArrayAdapter<CharSequence> mClipSrcAdapter;

    private ClipPlayFragment mClipPlayFragment;

    private PlaylistEditor mPlaylistEditor;

    public static final int PLAYLIST_INDEX = 0x100;

    private EventBus mEventBus = EventBus.getDefault();

    @BindView(R.id.gauge_list_view)
    RecyclerView mGaugeListView;

    @BindView(R.id.clips_edit_view)
    ClipsEditView mClipsEditView;

    @BindView(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @BindView(R.id.enhance_action_bar)
    View mEnhanceActionBar;

    @BindView(R.id.btn_gauge)
    View btnGauge;

    @BindView(R.id.btn_music)
    View btnMusic;


//    @BindView(R.id.volume_value_view)
//    TextView mVolumeView;
//
//    @BindView(R.id.volume_seek_bar)
//    SeekBar mVolumeSeekBar;

    @BindView(R.id.btn_add_music)
    Button btnAddMusic;

    @BindView(R.id.btn_remove)
    View btnRemove;

    @BindView(R.id.spinner_theme)
    Spinner mThemeSpinner;

    @BindView(R.id.spinner_length)
    Spinner mLengthSpinner;

    @BindView(R.id.spinner_clip_src)
    Spinner mClipSrcSpinner;

    @BindView(R.id.style_radio_group)
    RadioGroup mStyleRadioGroup;

    @OnClick(R.id.btn_music)
    void onClickMusic(View view) {
        btnGauge.setSelected(false);
//        btnRemix.setSelected(false);
        view.setSelected(!view.isSelected());
        if (mMusicItem == null) {
            MusicDownloadActivity.launchForResult(this, REQUEST_CODE_ADD_MUSIC);
        } else {
            configureActionUI(ACTION_ADD_MUSIC, view.isSelected());
        }
        updateMusicUI();
    }

    @OnClick(R.id.btn_add_music)
    public void addMusic() {
        MusicDownloadActivity.launchForResult(this, REQUEST_CODE_ADD_MUSIC);
    }

    @OnClick(R.id.btn_remove)
    public void removeMusic() {
        mMusicItem = null;
        getClipPlayFragment().setAudioUrl(null);
        updateMusicUI();
    }

    @OnClick(R.id.btn_gauge)
    public void showGauge(View view) {
        btnMusic.setSelected(false);
//        btnRemix.setSelected(false);
        mEventBus.post(new GaugeEvent(GaugeEvent.EVENT_WHAT_SHOW, true));
        view.setSelected(!view.isSelected());
        configureActionUI(ACTION_OVERLAY, view.isSelected());
    }

    @OnClick(R.id.btnThemeOff)
    public void onBtnThemeOffClicked() {
        mEventBus.post(new GaugeEvent(GaugeEvent.EVENT_WHAT_CHANGE_THEME, "NA"));
    }

    @OnClick(R.id.btnThemeDefault)
    public void onBtnThemeDefaultClicked() {
        mEventBus.post(new GaugeEvent(GaugeEvent.EVENT_WHAT_CHANGE_THEME, "default"));
        GaugeSettingManager.getManager().saveTheme("default");
    }

    @OnClick(R.id.btnThemeNeo)
    public void onBtnThemeNeoClicked() {
        mEventBus.post(new GaugeEvent(GaugeEvent.EVENT_WHAT_CHANGE_THEME, "neo"));
        GaugeSettingManager.getManager().saveTheme("neo");
    }

    @OnClick({R.id.btn_add_video, R.id.btn_add_video_extra})
    public void showClipChooser() {
        btnMusic.setSelected(false);
//        btnRemix.setSelected(false);
        btnGauge.setSelected(false);
        if (mViewAnimator.getDisplayedChild() != ACTION_ADD_VIDEO) {
            configureActionUI(ACTION_NONE, false);
        }
        Intent intent = new Intent(getActivity(), ClipChooserActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ENHANCE);
    }

//    @OnClick(R.id.btn_smart_remix)
//    void showSmartRemix(View view) {
//        btnMusic.setSelected(false);
//        btnGauge.setSelected(false);
//        view.setSelected(!view.isSelected());
//        configureActionUI(ACTION_SMART_REMIX, view.isSelected());
//        if (mThemeAdapter == null) {
//            mThemeAdapter = ArrayAdapter.createFromResource(getActivity(),
//                R.array.theme_remix,
//                R.layout.layout_remix_spinner);
//            mThemeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            mThemeSpinner.setAdapter(mThemeAdapter);
//        }
//
//        if (mLengthAdapter == null) {
//            mLengthAdapter = ArrayAdapter.createFromResource(getActivity(),
//                R.array.theme_length,
//                R.layout.layout_remix_spinner);
//            mLengthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            mLengthSpinner.setAdapter(mLengthAdapter);
//        }
//
//        if (mClipSrcAdapter == null) {
//            mClipSrcAdapter = ArrayAdapter.createFromResource(getActivity(),
//                R.array.theme_clip_src,
//                R.layout.layout_remix_spinner);
//            mClipSrcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            mClipSrcSpinner.setAdapter(mClipSrcAdapter);
//        }
//    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.layout_enhance, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Activity activity = getActivity();
        if (activity instanceof ClipPlayFragment.ClipPlayFragmentContainer) {
            mClipPlayFragment = ((ClipPlayFragment.ClipPlayFragmentContainer) activity).getClipPlayFragment();
//            Logger.t(TAG).d("clip play fragment: " + mClipPlayFragment);
        }
        mClipsEditView.setVisibility(View.INVISIBLE);
        mPlaylistEditor = new PlaylistEditor(mVdbRequestQueue, PLAYLIST_INDEX);
        mPlaylistEditor.build(ClipSetManager.CLIP_SET_TYPE_ENHANCE, new PlaylistEditor.OnBuildCompleteListener() {
            @Override
            public void onBuildComplete(ClipSet clipSet) {
//                ClipSetManager.getManager().updateClipSet(ClipSetManager.CLIP_SET_TYPE_ENHANCE, clipSet);
                PlaylistUrlProvider urlProvider = new PlaylistUrlProvider(mVdbRequestQueue, mPlaylistEditor.getPlaylistId());
                mClipPlayFragment.setUrlProvider(urlProvider);
                Logger.t(TAG).d("enhance clipset: \n" + clipSet.toString());

            }
        });
        mClipsEditView.setVisibility(View.VISIBLE);
        mEventBus.register(mPlaylistEditor);

        configEnhanceView();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mEventBus.unregister(mPlaylistEditor);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_ENHANCE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ArrayList<Clip> clips = data.getParcelableArrayListExtra(EnhancementActivity.EXTRA_CLIPS_TO_APPEND);
                    Logger.t(TAG).d("append clips: " + clips.size());
                    if (!mClipsEditView.appendSharableClips(clips)) {
                        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                            .content(R.string.resolution_not_correct)
                            .positiveText(android.R.string.ok)
                            .build();
                        dialog.show();
                    }
//                    mClipPlayFragment.setPosition(0);
                }
                break;
            case REQUEST_CODE_ADD_MUSIC:
                Logger.t(TAG).d("Resultcode: " + resultCode + " data: " + data);
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mMusicItem = MusicItem.fromBundle(data.getBundleExtra("music.item"));
                    updateMusicUI();
                    getClipPlayFragment().setAudioUrl(mMusicItem.localPath);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }


    void updateMusicUI() {
        if (mMusicItem == null) {
            btnAddMusic.setText(R.string.add_music);
            btnRemove.setVisibility(View.GONE);
        } else {
            btnAddMusic.setText(R.string.swap);
            btnRemove.setVisibility(View.VISIBLE);
        }

    }


    void configureActionUI(int child, boolean isShow) {
        if (isShow && (child != ACTION_NONE)) {
            mViewAnimator.setVisibility(View.VISIBLE);
            mViewAnimator.setDisplayedChild(child);
            mClipsEditView.setVisibility(View.GONE);
        } else {
            mViewAnimator.setVisibility(View.GONE);
            mClipsEditView.setVisibility(View.VISIBLE);
        }
    }


    void configEnhanceView() {
        mClipsEditView.setClipIndex(ClipSetManager.CLIP_SET_TYPE_ENHANCE);
        mClipsEditView.setOnClipEditListener(new ClipsEditView.OnClipEditListener() {
            @Override
            public void onClipSelected(int position, Clip clip) {
                getActivity().setTitle(R.string.trim);
                mEnhanceActionBar.setVisibility(View.INVISIBLE);
                ClipSetPos clipSetPos = new ClipSetPos(position, clip.editInfo.selectedStartValue);
                mEventBus.post(new ClipSetPosChangeEvent(clipSetPos, TAG));
            }

            @Override
            public void onClipMoved(int fromPosition, final int toPosition, final Clip clip) {

                int selectedPosition = mClipsEditView.getSelectedPosition();
                ClipSetPos clipSetPos = getClipPlayFragment().getClipSetPos();
                if (selectedPosition == -1) {
                    getClipPlayFragment().showClipPosThumbnail(clip, clip.editInfo.selectedStartValue);
                } else if (selectedPosition != clipSetPos.getClipIndex()) {
                    ClipSetPos newClipSetPos = new ClipSetPos(selectedPosition, clip.editInfo.selectedStartValue);
                    getClipPlayFragment().setClipSetPos(newClipSetPos, false);
                }

            }

            @Override
            public void onClipsAppended(List<Clip> clips, int clipCount) {
                if (clips == null) {
                    return;
                }

                if (clipCount > 0 && mViewAnimator.getDisplayedChild() == ACTION_ADD_VIDEO) {
                    btnGauge.setEnabled(true);
                    btnMusic.setEnabled(true);
                    configureActionUI(ACTION_NONE, false);
                }
            }

            @Override
            public void onClipRemoved(int clipCount) {

                if (clipCount == 0) {
                    btnGauge.setEnabled(false);
                    btnMusic.setEnabled(false);
                    configureActionUI(ACTION_ADD_VIDEO, true);
                }
            }

            @Override
            public void onExitEditing() {
                getActivity().setTitle(R.string.enhance);
                mEnhanceActionBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStartTrimming() {

            }

            @Override
            public void onTrimming(Clip clip) {

            }

            @Override
            public void onStopTrimming(Clip clip) {
                int selectedPosition = mClipsEditView.getSelectedPosition();
                if (selectedPosition == ClipsEditView.POSITION_UNKNOWN) {
                    return;
                }

            }
        });


        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mGaugeListView.setLayoutManager(layoutManager);
        mGaugeListAdapter = new GaugeListAdapter(new GaugeListAdapter.OnGaugeItemChangedListener() {
            @Override
            public void onGaugeItemChanged(GaugeInfoItem item) {
                mEventBus.post(new GaugeEvent(GaugeEvent.EVENT_WHAT_UPDATE_SETTING, item));
                GaugeSettingManager.getManager().saveSetting(item);
            }
        });
        mGaugeListView.setAdapter(mGaugeListAdapter);
        configureActionUI(ACTION_NONE, false);
    }


    public JSONObject getGaugeSettings() {
        JSONObject jsonObject = mGaugeListAdapter.toJSOptions();
        int checkedId = mStyleRadioGroup.getCheckedRadioButtonId();
        try {
            switch (checkedId) {
                case R.id.btnThemeOff:
                    jsonObject.put("theme", "NA");
                    break;
                case R.id.btnThemeDefault:
                    jsonObject.put("theme", "default");
                    break;
                case R.id.btnThemeNeo:
                    jsonObject.put("theme", "neo");
                    break;
            }
        } catch (JSONException e) {
            Logger.t(TAG).e(e, "");
        }
        return jsonObject;
    }


    public int getAudioID() {
        if (mMusicItem != null) {
            return mMusicItem.id;
        } else {
            return DEFAULT_AUDIO_ID;
        }
    }

    public ClipPlayFragment getClipPlayFragment() {
        return ((ClipPlayFragment.ClipPlayFragmentContainer) getActivity()).getClipPlayFragment();
    }


}
