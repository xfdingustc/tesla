package com.waylens.hachi.ui.clips.enhance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
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
import com.waylens.hachi.ui.clips.ClipChooserActivity;
import com.waylens.hachi.ui.clips.ClipPlayActivity;
import com.waylens.hachi.ui.clips.EnhancementActivity;
import com.waylens.hachi.ui.clips.MusicDownloadActivity;
import com.waylens.hachi.ui.clips.editor.clipseditview.ClipsEditView;
import com.waylens.hachi.ui.clips.player.GaugeInfoItem;
import com.waylens.hachi.ui.clips.player.PlaylistEditor;
import com.waylens.hachi.ui.clips.player.PlaylistUrlProvider;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor2;
import com.waylens.hachi.ui.entities.MusicItem;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;
import com.waylens.hachi.vdb.ClipSetPos;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class EnhanceActivity2 extends ClipPlayActivity {
    private static final String TAG = EnhanceActivity2.class.getSimpleName();
    private static final String EXTRA_PLAYLIST_ID = "playlist_id";

    private static final int REQUEST_CODE_ENHANCE = 1000;
    private static final int REQUEST_CODE_ADD_MUSIC = 1001;

    private static final int ACTION_NONE = -1;
    private static final int ACTION_OVERLAY = 0;
    private static final int ACTION_ADD_VIDEO = 1;
    private static final int ACTION_ADD_MUSIC = 2;
    private static final int ACTION_SMART_REMIX = 3;

    private MusicItem mMusicItem;
    private int mPlaylistId;
    private GaugeListAdapter mGaugeListAdapter;

    public static void launch(Activity activity, int playlistId) {
        Intent intent = new Intent(activity, EnhanceActivity2.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        activity.startActivity(intent);
    }


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
        mClipPlayFragment.setAudioUrl(null);
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
        Intent intent = new Intent(this, ClipChooserActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ENHANCE);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStop() {
        super.onStop();
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
                        MaterialDialog dialog = new MaterialDialog.Builder(this)
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
                    mClipPlayFragment.setAudioUrl(mMusicItem.localPath);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    protected void init() {
        super.init();
        mPlaylistId = getIntent().getIntExtra(EXTRA_PLAYLIST_ID, -1);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_enhance2);
        setupToolbar();
        mPlaylistEditor = new PlayListEditor2(mVdbRequestQueue, mPlaylistId);
        mPlaylistEditor.reconstruct();
        embedVideoPlayFragment();
        embedEnhanceFragment();
        mClipsEditView.setVisibility(View.INVISIBLE);


        mClipsEditView.setVisibility(View.VISIBLE);
        mEventBus.register(mPlaylistEditor);
        configEnhanceView();
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.enhance);

    }


    private void embedEnhanceFragment() {

    }

    private void configureActionUI(int child, boolean isShow) {
        if (isShow && (child != ACTION_NONE)) {
            mViewAnimator.setVisibility(View.VISIBLE);
            mViewAnimator.setDisplayedChild(child);
            mClipsEditView.setVisibility(View.GONE);
        } else {
            mViewAnimator.setVisibility(View.GONE);
            mClipsEditView.setVisibility(View.VISIBLE);
        }
    }

    private void updateMusicUI() {
        if (mMusicItem == null) {
            btnAddMusic.setText(R.string.add_music);
            btnRemove.setVisibility(View.GONE);
        } else {
            btnAddMusic.setText(R.string.swap);
            btnRemove.setVisibility(View.VISIBLE);
        }

    }

    private void configEnhanceView() {
        mClipsEditView.setClipIndex(mPlaylistEditor.getPlaylistId());
        mClipsEditView.setOnClipEditListener(new ClipsEditView.OnClipEditListener() {
            @Override
            public void onClipSelected(int position, Clip clip) {
                getToolbar().setTitle(R.string.trim);
                mEnhanceActionBar.setVisibility(View.INVISIBLE);
                ClipSetPos clipSetPos = new ClipSetPos(position, clip.editInfo.selectedStartValue);
                mEventBus.post(new ClipSetPosChangeEvent(clipSetPos, TAG));
            }

            @Override
            public void onClipMoved(int fromPosition, final int toPosition, final Clip clip) {

                int selectedPosition = mClipsEditView.getSelectedPosition();
                ClipSetPos clipSetPos = mClipPlayFragment.getClipSetPos();
                if (selectedPosition == -1) {
                    mClipPlayFragment.showClipPosThumbnail(clip, clip.editInfo.selectedStartValue);
                } else if (selectedPosition != clipSetPos.getClipIndex()) {
                    ClipSetPos newClipSetPos = new ClipSetPos(selectedPosition, clip.editInfo.selectedStartValue);
                    mClipPlayFragment.setClipSetPos(newClipSetPos, false);
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
            public void onClipRemoved(Clip clip, int position, int clipCount) {

                if (clipCount == 0) {
                    btnGauge.setEnabled(false);
                    btnMusic.setEnabled(false);
                    configureActionUI(ACTION_ADD_VIDEO, true);
                }
            }

            @Override
            public void onExitEditing() {
                getToolbar().setTitle(R.string.enhance);
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


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
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
}
