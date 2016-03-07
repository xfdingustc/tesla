package com.waylens.hachi.ui.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.ui.activities.ClipChooserActivity;
import com.waylens.hachi.ui.activities.MusicDownloadActivity;
import com.waylens.hachi.ui.adapters.GaugeListAdapter;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.ui.fragments.clipplay.CameraVideoPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay.VideoPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.PlaylistEditor;
import com.waylens.hachi.ui.fragments.clipplay2.PlaylistUrlProvider;
import com.waylens.hachi.ui.fragments.clipplay2.UrlProvider;
import com.waylens.hachi.ui.views.clipseditview.ClipsEditView;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;
import com.waylens.hachi.vdb.Playlist;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Richard on 12/18/15.
 */
public class EnhancementFragment extends BaseFragment implements FragmentNavigator, ClipsEditView.OnClipEditListener {
    private static final int REQUEST_CODE_ENHANCE = 1000;

    private int mClipSetIndex;
    private PlaylistEditor mPlaylistEditor;

    private int mAudioID = -1;
    private String mAudioPath;

    CameraVideoPlayFragment mVideoPlayFragment;

    private ClipPlayFragment mClipPlayFragment;

    @Bind(R.id.gauge_list_view)
    RecyclerView mGaugeListView;

    @Bind(R.id.clips_edit_view)
    ClipsEditView mClipsEditView;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

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

    @Bind(R.id.offset_value_view)
    TextView mOffsetView;

    @Bind(R.id.music_offset_seek_bar)
    SeekBar mOffsetSeekBar;

    String[] supportedGauges;
    int[] gaugeDefaultSizes;
    GaugeListAdapter mGaugeListAdapter;

    @OnClick(R.id.btn_music)
    void onClickMusic(View view) {
        btnGauge.setSelected(false);
        btnRemix.setSelected(false);
        view.setSelected(!view.isSelected());
        configureActionUI(1, view.isSelected());
    }

    @OnClick(R.id.btn_add_music)
    void adMusic() {
        MusicDownloadActivity.launch(getActivity());
    }

    @OnClick(R.id.btn_gauge)
    void showGauge(View view) {
        btnMusic.setSelected(false);
        btnRemix.setSelected(false);
        view.setSelected(!view.isSelected());
        if (mGaugeListAdapter == null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            mGaugeListView.setLayoutManager(layoutManager);
            mGaugeListAdapter = new GaugeListAdapter(supportedGauges, gaugeDefaultSizes);
            mGaugeListView.setAdapter(mGaugeListAdapter);
        }
        configureActionUI(0, view.isSelected());
    }

    void configureActionUI(int child, boolean isShow) {
        if (isShow) {
            mViewAnimator.setVisibility(View.VISIBLE);
            mViewAnimator.setDisplayedChild(child);
            mClipsEditView.setVisibility(View.GONE);
        } else {
            mViewAnimator.setVisibility(View.GONE);
            mClipsEditView.setVisibility(View.VISIBLE);
        }
    }


    @OnClick(R.id.btn_add_video)
    void showClipChooser() {
        Intent intent = new Intent(getActivity(), ClipChooserActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ENHANCE);
    }

    @OnClick(R.id.btn_smart_remix)
    void showSmartRemix(View view) {
        btnMusic.setSelected(false);
        btnGauge.setSelected(false);
        view.setSelected(!view.isSelected());
        configureActionUI(2, view.isSelected());
    }

    public static EnhancementFragment newInstance(SharableClip sharableClip) {
        Bundle args = new Bundle();
        EnhancementFragment fragment = new EnhancementFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static EnhancementFragment newInstance(Playlist playlist) {
        Bundle args = new Bundle();
        EnhancementFragment fragment = new EnhancementFragment();
        fragment.setArguments(args);
        return fragment;
    }


    public static EnhancementFragment newInstance(int clipSetIndex) {
        Bundle args = new Bundle();
        EnhancementFragment fragment = new EnhancementFragment();
        fragment.setArguments(args);
        fragment.mClipSetIndex = clipSetIndex;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mVdbRequestQueue = Snipe.newRequestQueue();
        supportedGauges = getResources().getStringArray(R.array.supported_gauges);
        gaugeDefaultSizes = getResources().getIntArray(R.array.gauges_default_size);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_enhance, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPlaylistEditor = new PlaylistEditor(getActivity(), mVdtCamera, mClipSetIndex, 0x100);
        mPlaylistEditor.build(new PlaylistEditor.OnBuildCompleteListener() {
            @Override
            public void onBuildComplete(ClipSet clipSet) {
                embedVideoPlayFragment();
                mClipsEditView.setClipIndex(mClipSetIndex);
            }
        });
        mClipsEditView.setOnClipEditListener(this);

        mVolumeView.setText("100");
        mVolumeSeekBar.setMax(100);
        mVolumeSeekBar.setProgress(100);
        mOffsetView.setText(DateUtils.formatElapsedTime(0));
        mOffsetSeekBar.setEnabled(mAudioID != -1);

    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, new IntentFilter("choose-bg-music"));
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mVideoPlayFragment != null) {
            getFragmentManager().beginTransaction().remove(mVideoPlayFragment).commitAllowingStateLoss();
            mVideoPlayFragment = null;
        }
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
    }

    private void embedVideoPlayFragment() {
        ClipPlayFragment.Config config = new ClipPlayFragment.Config();
        config.clipMode = ClipPlayFragment.Config.ClipMode.MULTI;

        UrlProvider vdtUriProvider = new PlaylistUrlProvider(mVdbRequestQueue, mPlaylistEditor.getPlayListID());
        mClipPlayFragment = ClipPlayFragment.newInstance(getCamera(), mClipSetIndex,
                vdtUriProvider,
                config);
        mClipPlayFragment.setShowsDialog(false);
        getChildFragmentManager().beginTransaction().replace(R.id.enhance_fragment_content, mClipPlayFragment).commit();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_enhance, menu);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_ENHANCE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ArrayList<Clip> clips = data.getParcelableArrayListExtra("clips.more");
                    Log.e("test", "Clips: " + clips);
                    mClipsEditView.appendSharableClips(clips);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    void close() {
        getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public boolean onInterceptBackPressed() {
        if (VideoPlayFragment.fullScreenPlayer != null) {
            VideoPlayFragment.fullScreenPlayer.setFullScreen(false);
            return true;
        }
        close();
        return true;
    }

    @Override
    public void onClipSelected(int position, Clip clip) {
        getActivity().setTitle(R.string.trim);
        mClipPlayFragment.setActiveClip(position, clip);
    }

    @Override
    public void onClipMoved(int fromPosition, int toPosition) {
        Log.e("test", String.format("onClipMoved[%d, %d]", fromPosition, toPosition));
        mPlaylistEditor.move(fromPosition, toPosition, new PlaylistEditor.OnMoveCompletedListener() {
            @Override
            public void onMoveCompleted() {
                mClipPlayFragment.notifyClipSetChanged();
            }
        });

    }

    @Override
    public void onClipsAppended(List<Clip> clips) {
        if (clips == null) {
            return;
        }
        mPlaylistEditor.appendClips(clips, new PlaylistEditor.OnBuildCompleteListener() {
            @Override
            public void onBuildComplete(ClipSet clipSet) {
                mClipPlayFragment.setClipSet(clipSet);
            }
        });
    }

    @Override
    public void onClipRemoved(Clip clip, int position) {
        mPlaylistEditor.delete(position, new PlaylistEditor.OnDeleteCompleteListener() {
            @Override
            public void onDeleteComplete() {
                mClipPlayFragment.notifyClipSetChanged();
            }
        });
    }

    @Override
    public void onExitEditing() {
        getActivity().setTitle(R.string.enhance);
    }

    @Override
    public void onStartTrimming() {
        //TODO
    }

    @Override
    public void onTrimming(Clip clip, int flag, long value) {
//        Logger.t("test").d("Clip selected time" + mPlaylistEditor.getClipSet().getTotalSelectedLengthMs());
        mClipPlayFragment.showClipPosThumbnail(clip, value);
        mClipPlayFragment.notifyClipSetChanged();
    }

    @Override
    public void onStopTrimming() {
        int selectedPosition = mClipsEditView.getSelectedPosition();
        if (selectedPosition == ClipsEditView.POSITION_UNKNOWN) {
            return;
        }
        Clip clip = getClipSet().getClip(selectedPosition);
        mPlaylistEditor.trimClip(selectedPosition, clip.editInfo.selectedStartValue, clip.editInfo.selectedEndValue,
                new PlaylistEditor.OnTrimCompletedListener() {
                    @Override
                    public void onTrimCompleted(ClipSet clipSet) {
                        mClipPlayFragment.setClipSet(clipSet);
                        //mClipPlayFragment.prepare(0);
                    }
                });

    }

    private ClipSet getClipSet() {
        return ClipSetManager.getManager().getClipSet(mClipSetIndex);
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mAudioID = intent.getIntExtra("music-id", 0);
            String name = intent.getStringExtra("name");
            mAudioPath = intent.getStringExtra("path");
            if (mVideoPlayFragment != null) {
                getFragmentManager().beginTransaction().remove(mVideoPlayFragment).commit();
                mVideoPlayFragment = null;
            }
        }
    };
}
