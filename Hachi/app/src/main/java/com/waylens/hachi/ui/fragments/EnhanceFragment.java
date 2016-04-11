package com.waylens.hachi.ui.fragments;

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
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.ClipSetChangeEvent;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.eventbus.events.GaugeEvent;
import com.waylens.hachi.ui.activities.ClipChooserActivity;
import com.waylens.hachi.ui.activities.EnhancementActivity;
import com.waylens.hachi.ui.activities.MusicDownloadActivity;
import com.waylens.hachi.ui.adapters.GaugeListAdapter;
import com.waylens.hachi.ui.entities.MusicItem;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.GaugeInfoItem;
import com.waylens.hachi.ui.fragments.clipplay2.PlaylistEditor;
import com.waylens.hachi.ui.views.clipseditview.ClipsEditView;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;
import com.waylens.hachi.vdb.ClipSetPos;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Richard on 3/22/16.
 */
public class EnhanceFragment extends BaseFragment implements ClipsEditView.OnClipEditListener {
    private static final String TAG = "EnhanceFragment";

    private static final int REQUEST_CODE_ENHANCE = 1000;
    private static final int REQUEST_CODE_ADD_MUSIC = 1001;

    public static final int DEFAULT_AUDIO_ID = -1;

    private static final int ACTION_NONE = -1;
    private static final int ACTION_OVERLAY = 0;
    private static final int ACTION_ADD_VIDEO = 1;
    private static final int ACTION_ADD_MUSIC = 2;
    private static final int ACTION_SMART_REMIX = 3;

    String[] supportedGauges;
    GaugeListAdapter mGaugeListAdapter;

    MusicItem mMusicItem;

    ArrayAdapter<CharSequence> mThemeAdapter;
    ArrayAdapter<CharSequence> mLengthAdapter;
    ArrayAdapter<CharSequence> mClipSrcAdapter;

//    ClipPlayFragment mClipPlayFragment;

    private PlaylistEditor mPlaylistEditor;

    private EventBus mEventBus = EventBus.getDefault();

    @Bind(R.id.gauge_list_view)
    RecyclerView mGaugeListView;

    @Bind(R.id.clips_edit_view)
    ClipsEditView mClipsEditView;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.enhance_action_bar)
    View mEnhanceActionBar;

    @Bind(R.id.btn_gauge)
    View btnGauge;

    @Bind(R.id.btn_music)
    View btnMusic;

    @Bind(R.id.btn_smart_remix)
    View btnRemix;

    @Bind(R.id.volume_value_view)
    TextView mVolumeView;

    @Bind(R.id.volume_seek_bar)
    SeekBar mVolumeSeekBar;

    @Bind(R.id.btn_add_music)
    Button btnAddMusic;

    @Bind(R.id.btn_remove)
    View btnRemove;

    @Bind(R.id.spinner_theme)
    Spinner mThemeSpinner;

    @Bind(R.id.spinner_length)
    Spinner mLengthSpinner;

    @Bind(R.id.spinner_clip_src)
    Spinner mClipSrcSpinner;

    @Bind(R.id.style_radio_group)
    RadioGroup mStyleRadioGroup;

    @OnClick(R.id.btn_music)
    void onClickMusic(View view) {
        btnGauge.setSelected(false);
        btnRemix.setSelected(false);
        view.setSelected(!view.isSelected());
        configureActionUI(ACTION_ADD_MUSIC, view.isSelected());
        updateMusicUI();
    }

    @OnClick(R.id.btn_add_music)
    void addMusic() {
        MusicDownloadActivity.launchForResult(this, REQUEST_CODE_ADD_MUSIC);
    }

    @OnClick(R.id.btn_remove)
    void removeMusic() {
        mMusicItem = null;
        getClipPlayFragment().setAudioUrl(null);
        updateMusicUI();
    }

    @OnClick(R.id.btn_gauge)
    void showGauge(View view) {
        btnMusic.setSelected(false);
        btnRemix.setSelected(false);
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
    }

    @OnClick(R.id.btnThemeNeo)
    public void onBtnThemeNeoClicked() {
        mEventBus.post(new GaugeEvent(GaugeEvent.EVENT_WHAT_CHANGE_THEME, "neo"));
    }

    @OnClick({R.id.btn_add_video, R.id.btn_add_video_extra})
    void showClipChooser() {
        btnMusic.setSelected(false);
        btnRemix.setSelected(false);
        btnGauge.setSelected(false);
        if (mViewAnimator.getDisplayedChild() != ACTION_ADD_VIDEO) {
            configureActionUI(ACTION_NONE, false);
        }
        Intent intent = new Intent(getActivity(), ClipChooserActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ENHANCE);
    }

    @OnClick(R.id.btn_smart_remix)
    void showSmartRemix(View view) {
        btnMusic.setSelected(false);
        btnGauge.setSelected(false);
        view.setSelected(!view.isSelected());
        configureActionUI(ACTION_SMART_REMIX, view.isSelected());
        if (mThemeAdapter == null) {
            mThemeAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.theme_remix,
                R.layout.layout_remix_spinner);
            mThemeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mThemeSpinner.setAdapter(mThemeAdapter);
        }

        if (mLengthAdapter == null) {
            mLengthAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.theme_length,
                R.layout.layout_remix_spinner);
            mLengthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mLengthSpinner.setAdapter(mLengthAdapter);
        }

        if (mClipSrcAdapter == null) {
            mClipSrcAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.theme_clip_src,
                R.layout.layout_remix_spinner);
            mClipSrcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mClipSrcSpinner.setAdapter(mClipSrcAdapter);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportedGauges = getResources().getStringArray(R.array.supported_gauges);
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
//            mClipPlayFragment = ((ClipPlayFragment.ClipPlayFragmentContainer) activity).getClipPlayFragment();
//            Logger.t(TAG).d("clip play fragment: " + mClipPlayFragment);
        }
        mClipsEditView.setVisibility(View.INVISIBLE);
        mPlaylistEditor = new PlaylistEditor(getActivity(), mVdtCamera, 0x100);
        mClipsEditView.setVisibility(View.VISIBLE);
//        mPlaylistEditor.build(ClipSetManager.CLIP_SET_TYPE_ENHANCE_EDITING, new PlaylistEditor.OnBuildCompleteListener() {
//            @Override
//            public void onBuildComplete(ClipSet clipSet) {
//
//            }
//        });
        configEnhanceView();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_ENHANCE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ArrayList<Clip> clips = data.getParcelableArrayListExtra(EnhancementActivity.EXTRA_CLIPS_TO_APPEND);
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
            btnAddMusic.setText(R.string.btn_swap);
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
        mClipsEditView.setOnClipEditListener(this);

        mVolumeView.setText("100");
        mVolumeSeekBar.setMax(100);
        mVolumeSeekBar.setProgress(100);

        mVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mVolumeView.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mGaugeListView.setLayoutManager(layoutManager);
        mGaugeListAdapter = new GaugeListAdapter(supportedGauges, new GaugeListAdapter.OnGaugeItemChangedListener() {
            @Override
            public void onGaugeItemChanged(GaugeInfoItem item) {
                mEventBus.post(new GaugeEvent(GaugeEvent.EVENT_WHAT_UPDATE_SETTING, item));
            }
        });
        mGaugeListView.setAdapter(mGaugeListAdapter);
        configureActionUI(ACTION_NONE, false);
    }


    public String getGaugeSettings() {
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
        return jsonObject.toString();
    }

    public int getSelectedPosition() {
        return mClipsEditView.getSelectedPosition();
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

    @Override
    public void onClipSelected(int position, Clip clip) {
        getActivity().setTitle(R.string.trim);
        mEnhanceActionBar.setVisibility(View.INVISIBLE);
        ClipSetPos clipSetPos = new ClipSetPos(position, clip.getStartTimeMs());

//        mClipPlayFragment.setClipSetPos(clipSetPos, true);
        mEventBus.post(new ClipSetPosChangeEvent(clipSetPos, TAG));
    }


    @Override
    public void onClipMoved(int fromPosition, final int toPosition, final Clip clip) {

        mPlaylistEditor.move(fromPosition, toPosition, new PlaylistEditor.OnMoveCompletedListener() {
            @Override
            public void onMoveCompleted(ClipSet clipSet) {
                int selectedPosition = mClipsEditView.getSelectedPosition();
                ClipSetPos clipSetPos = getClipPlayFragment().getClipSetPos();
                if (selectedPosition != clipSetPos.getClipIndex()) {
                    ClipSetPos newClipSetPos = new ClipSetPos(selectedPosition, clip.getStartTimeMs());
                    getClipPlayFragment().setClipSetPos(newClipSetPos, false);
                }
                if (selectedPosition == -1 && toPosition == 0) {
                    getClipPlayFragment().showClipPosThumbnail(clip, clip.getStartTimeMs());
                }
                mEventBus.post(new ClipSetChangeEvent(ClipSetManager.CLIP_SET_TYPE_ENHANCE));
            }
        });

    }

    @Override
    public void onClipsAppended(List<Clip> clips, int clipCount) {
        if (clips == null) {
            return;
        }
        mPlaylistEditor.appendClips(clips, new PlaylistEditor.OnBuildCompleteListener() {
            @Override
            public void onBuildComplete(ClipSet clipSet) {
                mEventBus.post(new ClipSetChangeEvent(ClipSetManager.CLIP_SET_TYPE_ENHANCE));
            }
        });
        if (clipCount > 0
            && mViewAnimator.getDisplayedChild() == ACTION_ADD_VIDEO) {
            btnGauge.setEnabled(true);
            btnMusic.setEnabled(true);
            configureActionUI(ACTION_NONE, false);
        }
    }

    @Override
    public void onClipRemoved(Clip clip, int position, int clipCount) {
        mPlaylistEditor.delete(position, new PlaylistEditor.OnDeleteCompleteListener() {
            @Override
            public void onDeleteComplete() {
                mEventBus.post(new ClipSetChangeEvent(ClipSetManager.CLIP_SET_TYPE_ENHANCE));
            }
        });
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
        //TODO
    }

    @Override
    public void onTrimming(Clip clip, int flag, long value) {
        mEventBus.post(new ClipSetChangeEvent(ClipSetManager.CLIP_SET_TYPE_ENHANCE));
    }

    @Override
    public void onStopTrimming(Clip clip) {
        int selectedPosition = mClipsEditView.getSelectedPosition();
        if (selectedPosition == ClipsEditView.POSITION_UNKNOWN) {
            return;
        }
        mPlaylistEditor.trimClip(selectedPosition, clip,
            new PlaylistEditor.OnTrimCompletedListener() {
                @Override
                public void onTrimCompleted(ClipSet clipSet) {
                    mEventBus.post(new ClipSetChangeEvent(ClipSetManager.CLIP_SET_TYPE_ENHANCE));
                }
            });

    }


}
