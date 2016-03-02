package com.waylens.hachi.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.ui.activities.ClipChooserActivity;
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

    private ArrayList<Clip> mClips;
    private PlaylistEditor mPlaylistEditor;

    private int mAudioID;
    private String mAudioPath;

    CameraVideoPlayFragment mVideoPlayFragment;

    private ClipPlayFragment mClipPlayFragment;

    @Bind(R.id.enhance_root_view)
    LinearLayout mEnhanceRootView;

    @Bind(R.id.gauge_list_view)
    RecyclerView mGaugeListView;

    @Bind(R.id.clips_edit_view)
    ClipsEditView mClipsEditView;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    String[] supportedGauges;
    int[] gaugeDefaultSizes;
    GaugeListAdapter mGaugeListAdapter;

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, new IntentFilter("choose-bg-music"));
        //clearExistingPlayList(0x100, null);

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

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    void onClickShare() {
        //getFragmentManager().beginTransaction().replace(R.id.root_container, ShareFragment.newInstance(mSharableClip, mAudioID)).commit();
    }

    @OnClick(R.id.btn_music)
    void onClickMusic() {
        //getFragmentManager().beginTransaction()
        //        .add(R.id.root_container, new MusicFragment())
        //        .addToBackStack(null)
        //        .commit();
    }

    @OnClick(R.id.btn_gauge)
    void showGauge(View view) {
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

    public static EnhancementFragment newInstance(ArrayList<Clip> clips) {
        Bundle args = new Bundle();
        EnhancementFragment fragment = new EnhancementFragment();
        fragment.setArguments(args);
        fragment.mClips = clips;
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
        View view = inflater.inflate(R.layout.fragment_enhance, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEnhanceRootView.requestDisallowInterceptTouchEvent(true);

        mPlaylistEditor = new PlaylistEditor(getActivity(), mVdtCamera, mClips, 0x100);
        mPlaylistEditor.build(new PlaylistEditor.OnBuildCompleteListener() {
            @Override
            public void onBuildComplete() {
                embedVideoPlayFragment();
                mClipsEditView.setClips(mPlaylistEditor.getClipSet().getClipList());
                //mClipsEditView.setClips(mClips);
            }
        });
        mClipsEditView.setOnClipEditListener(this);

    }

    private void embedVideoPlayFragment() {
        ClipPlayFragment.Config config = new ClipPlayFragment.Config();
        config.progressBarStyle = ClipPlayFragment.Config.PROGRESS_BAR_STYLE_SINGLE;
        config.showControlPanel = false;

        UrlProvider vdtUriProvider = new PlaylistUrlProvider(mVdbRequestQueue, mPlaylistEditor.getPlayListID());
        mClipPlayFragment = ClipPlayFragment.newInstance(getCamera(), mPlaylistEditor.getClipSet(), vdtUriProvider,
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
        if (requestCode != REQUEST_CODE_ENHANCE) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Log.e("test", "resultCode:" + resultCode);
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
            public void onBuildComplete() {
                mClipPlayFragment.notifyClipSetChanged();
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
        mClipPlayFragment.showClipPosThumbnail(clip, value);
        mClipPlayFragment.notifyClipSetChanged();
    }

    @Override
    public void onStopTrimming() {
        int selectedPosition = mClipsEditView.getSelectedPosition();
        if (selectedPosition == ClipsEditView.POSITION_UNKNOWN) {
            return;
        }
        Clip clip = mClips.get(selectedPosition);
        mPlaylistEditor.trimClip(selectedPosition, clip.editInfo.selectedStartValue, clip.editInfo.selectedEndValue,
                new PlaylistEditor.OnTrimCompletedListener() {
                    @Override
                    public void onTrimCompleted() {
                        mClipPlayFragment.notifyClipSetChanged();
                    }
                });
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
