package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.adapters.GaugeListAdapter;
import com.waylens.hachi.ui.entities.MusicItem;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.fragments.clipplay.VideoPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.GaugeInfoItem;
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
import butterknife.OnClick;

/**
 * Created by Richard on 12/18/15.
 */
public class EnhancementActivity2 extends BaseActivity implements FragmentNavigator,
        ClipsEditView.OnClipEditListener {
    private static final String TAG = EnhancementActivity2.class.getSimpleName();
    private static final int REQUEST_CODE_ENHANCE = 1000;
    private static final int REQUEST_CODE_ADD_MUSIC = 1001;

    private int mClipSetIndex;
    private PlaylistEditor mPlaylistEditor;

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



    String[] supportedGauges;
    int[] gaugeDefaultSizes;
    GaugeListAdapter mGaugeListAdapter;

    MusicItem mMusicItem;

    ArrayAdapter<CharSequence> mThemeAdapter;
    ArrayAdapter<CharSequence> mLengthAdapter;
    ArrayAdapter<CharSequence> mClipSrcAdapter;
    private VdbRequestQueue mVdbRequestQueue;
    private VdtCamera mVdtCamera;

    @OnClick(R.id.btn_music)
    void onClickMusic(View view) {
        btnGauge.setSelected(false);
        btnRemix.setSelected(false);
        view.setSelected(!view.isSelected());
        configureActionUI(1, view.isSelected());
        updateMusicUI();
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

    @OnClick(R.id.btn_add_music)
    void addMusic() {
        MusicDownloadActivity.launchForResult(this, REQUEST_CODE_ADD_MUSIC);
    }

    @OnClick(R.id.btn_remove)
    void removeMusic() {
        mMusicItem = null;
        mClipPlayFragment.setAudioUrl(null);
        updateMusicUI();
    }

    @OnClick(R.id.btn_gauge)
    void showGauge(View view) {
        btnMusic.setSelected(false);
        btnRemix.setSelected(false);
        mClipPlayFragment.showGaugeView(true);
        view.setSelected(!view.isSelected());
        if (mGaugeListAdapter == null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            mGaugeListView.setLayoutManager(layoutManager);
            mGaugeListAdapter = new GaugeListAdapter(supportedGauges, gaugeDefaultSizes, new GaugeListAdapter.OnGaugeItemChangedListener() {
                @Override
                public void onGaugeItemChanged(GaugeInfoItem item) {
                    mClipPlayFragment.updateGauge(item);
                }
            });
            mGaugeListView.setAdapter(mGaugeListAdapter);
        }
        configureActionUI(0, view.isSelected());
    }

    @OnClick(R.id.btnThemeDefault)
    public void onBtnThemeDefaultClicked() {
        mClipPlayFragment.setGaugeTheme("default");
    }

    @OnClick(R.id.btnThemeNeo)
    public void onBtnThemeNeoClicked() {
        mClipPlayFragment.setGaugeTheme("neo");
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
        Intent intent = new Intent(this, ClipChooserActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ENHANCE);
    }

    @OnClick(R.id.btn_smart_remix)
    void showSmartRemix(View view) {
        btnMusic.setSelected(false);
        btnGauge.setSelected(false);
        view.setSelected(!view.isSelected());
        configureActionUI(2, view.isSelected());
        if (mThemeAdapter == null) {
            mThemeAdapter = ArrayAdapter.createFromResource(this,
                    R.array.theme_remix,
                    R.layout.layout_remix_spinner);
            mThemeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mThemeSpinner.setAdapter(mThemeAdapter);
        }

        if (mLengthAdapter == null) {
            mLengthAdapter = ArrayAdapter.createFromResource(this,
                    R.array.theme_length,
                    R.layout.layout_remix_spinner);
            mLengthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mLengthSpinner.setAdapter(mLengthAdapter);
        }

        if (mClipSrcAdapter == null) {
            mClipSrcAdapter = ArrayAdapter.createFromResource(this,
                    R.array.theme_clip_src,
                    R.layout.layout_remix_spinner);
            mClipSrcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mClipSrcSpinner.setAdapter(mClipSrcAdapter);
        }
    }

//    public static EnhancementActivity2 newInstance(SharableClip sharableClip) {
//        Bundle args = new Bundle();
//        EnhancementActivity2 fragment = new EnhancementActivity2();
//        fragment.setArguments(args);
//        return fragment;
//    }

//    public static EnhancementActivity2 newInstance(Playlist playlist) {
//        Bundle args = new Bundle();
//        EnhancementActivity2 fragment = new EnhancementActivity2();
//        fragment.setArguments(args);
//        return fragment;
//    }


//    public static EnhancementActivity2 newInstance(int clipSetIndex) {
//        Bundle args = new Bundle();
//        EnhancementActivity2 fragment = new EnhancementActivity2();
//        fragment.setArguments(args);
//        fragment.mClipSetIndex = clipSetIndex;
//        return fragment;
//    }

    public static void launch(Activity activity, int clipSetIndex) {
        Intent intent = new Intent(activity, EnhancementActivity2.class);
        intent.putExtra("clipSetIndex", clipSetIndex);
        activity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setHasOptionsMenu(true);

        init();

    }

    @Override
    protected void init() {
        super.init();
        mVdbRequestQueue = Snipe.newRequestQueue();
        supportedGauges = getResources().getStringArray(R.array.supported_gauges);
        gaugeDefaultSizes = getResources().getIntArray(R.array.gauges_default_size);
        mClipSetIndex = getIntent().getIntExtra("clipSetIndex", ClipSetManager.CLIP_SET_TYPE_ENHANCE);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.fragment_enhance);
        mPlaylistEditor = new PlaylistEditor(this, mVdtCamera, mClipSetIndex, 0x100);
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
    }

    @Override
    public void setupToolbar() {
        mToolbar.setNavigationIcon(R.drawable.navbar_close);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        super.setupToolbar();

    }

    //    @Nullable
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        return createFragmentView(inflater, container, R.layout.fragment_enhance, savedInstanceState);
//    }






    private void embedVideoPlayFragment() {
        ClipPlayFragment.Config config = new ClipPlayFragment.Config();
        config.clipMode = ClipPlayFragment.Config.ClipMode.MULTI;

        UrlProvider vdtUriProvider = new PlaylistUrlProvider(mVdbRequestQueue, mPlaylistEditor.getPlayListID());
        mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, mClipSetIndex,
                vdtUriProvider,
                config);
        mClipPlayFragment.setShowsDialog(false);
        getFragmentManager().beginTransaction().replace(R.id.enhance_fragment_content, mClipPlayFragment).commit();
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
            case REQUEST_CODE_ADD_MUSIC:
                Logger.t(TAG).d("Resultcode: " + resultCode + " data: " + data);
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mMusicItem = MusicItem.fromBundle(data.getBundleExtra("music.item"));
                    updateMusicUI();
                    Log.e("test", "item: " + mMusicItem.localPath);
                    mClipPlayFragment.setAudioUrl(mMusicItem.localPath);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    void close() {
        //getFragmentManager().beginTransaction().remove(this).commit();
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
        mToolbar.setTitle(R.string.trim);
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
        mToolbar.setTitle(R.string.enhance);
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
}
