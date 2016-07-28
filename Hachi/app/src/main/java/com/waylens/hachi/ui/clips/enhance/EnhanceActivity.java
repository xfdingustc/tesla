package com.waylens.hachi.ui.clips.enhance;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.ViewAnimator;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.birbit.android.jobqueue.JobManager;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.GaugeSettingManager;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.download.DownloadJob;
import com.waylens.hachi.bgjob.download.event.DownloadEvent;
import com.waylens.hachi.eventbus.events.ClipSetChangeEvent;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.eventbus.events.GaugeEvent;
import com.waylens.hachi.library.vdb.Clip;
import com.waylens.hachi.library.vdb.ClipDownloadInfo;
import com.waylens.hachi.library.vdb.ClipSet;
import com.waylens.hachi.library.vdb.ClipSetManager;
import com.waylens.hachi.library.vdb.ClipSetPos;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.library.snipe.SnipeError;
import com.waylens.hachi.library.snipe.VdbResponse;
import com.waylens.hachi.library.snipe.toolbox.DownloadUrlRequest;
import com.waylens.hachi.ui.adapters.GaugeListAdapter;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.clips.ClipChooserActivity;
import com.waylens.hachi.ui.clips.ClipPlayActivity;
import com.waylens.hachi.ui.clips.MusicDownloadActivity;
import com.waylens.hachi.ui.clips.editor.clipseditview.ClipsEditView;
import com.waylens.hachi.ui.clips.player.GaugeInfoItem;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.clips.share.ShareActivity;
import com.waylens.hachi.ui.entities.MusicItem;


import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class EnhanceActivity extends ClipPlayActivity {
    private static final String TAG = EnhanceActivity.class.getSimpleName();

    private static final String EXTRA_CLIP_LIST = "clip_list";
    private static final String EXTRA_PLAYLIST_ID = "playlist_id";

    private static final int REQUEST_CODE_ENHANCE = 1000;
    private static final int REQUEST_CODE_ADD_MUSIC = 1001;

    private static final int LAUNCH_MODE_PLAYLIST = 100;


    public static final String EXTRA_CLIPS_TO_ENHANCE = "extra.clips.to.enhance";
    public static final String EXTRA_CLIPS_TO_APPEND = "extra.clips.to.append";
    public static final String EXTRA_LAUNCH_MODE = "extra.launch.mode";

    public static final int DEFAULT_AUDIO_ID = -1;

    private static final int ACTION_NONE = -1;
    private static final int ACTION_OVERLAY = 0;
    private static final int ACTION_ADD_VIDEO = 1;
    private static final int ACTION_ADD_MUSIC = 2;
    private static final int ACTION_SMART_REMIX = 3;

    public static final int PLAYLIST_INDEX = 0x100;

    private MusicItem mMusicItem;
    private int mPlaylistId;
    private GaugeListAdapter mGaugeListAdapter;

    private MaterialDialog mDownloadDialog;


    public static void launch(Activity activity, int playlistId) {
        Intent intent = new Intent(activity, EnhanceActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        intent.putExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_PLAYLIST);
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
    public void onClickMusic(View view) {
        btnGauge.setSelected(false);
//        btnRemix.setSelected(false);
//        view.setSelected(!view.isSelected());
        if (mMusicItem == null) {
            MusicDownloadActivity.launchForResult(this, REQUEST_CODE_ADD_MUSIC);
        } else {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(mMusicItem.title)
                .content(mMusicItem.description)
                .positiveText(R.string.change)
                .negativeText(R.string.remove)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        MusicDownloadActivity.launchForResult(EnhanceActivity.this, REQUEST_CODE_ADD_MUSIC);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        mClipPlayFragment.setAudioUrl(null);
                        mMusicItem = null;
                        btnMusic.setSelected(false);
                    }
                }).show();
        }
//        updateMusicUI();
    }

    @OnClick(R.id.btn_add_music)
    public void addMusic() {
        MusicDownloadActivity.launchForResult(this, REQUEST_CODE_ADD_MUSIC);
    }

    @OnClick(R.id.btn_remove)
    public void removeMusic() {
        mMusicItem = null;
        mClipPlayFragment.setAudioUrl(null);
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


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventDownload(DownloadEvent event) {
        switch (event.getWhat()) {
            case DownloadEvent.DOWNLOAD_WHAT_START:
                mDownloadDialog = new MaterialDialog.Builder(this)
                    .title(R.string.downloading)
                    .contentGravity(GravityEnum.CENTER)
                    .progress(false, 100, true)
                    .show();
                mDownloadDialog.setCanceledOnTouchOutside(false);
                break;
            case DownloadEvent.DOWNLOAD_WHAT_PROGRESS:
                if (mDownloadDialog != null) {
                    int progress = (Integer) event.getExtra();
                    mDownloadDialog.setProgress(progress);
                }
                break;
            case DownloadEvent.DOWNLOAD_WHAT_FINISHED:
                if (mDownloadDialog != null) {
                    mDownloadDialog.dismiss();
                }

                final String videoUrl = (String)event.getExtra();
                Snackbar snackbar = Snackbar.make(btnAddMusic, ("Stream has been download into " + videoUrl), Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.open, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(new File(videoUrl)), "video/mp4");
                        startActivity(intent);
                    }
                });
                snackbar.show();
                break;
        }
    }

    @Subscribe
    public void onEventClipSetChanged(ClipSetChangeEvent event) {
        Logger.t(TAG).d("on Clip Set chang event clip count: " + getClipSet().getCount());
        setupToolbar();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Logger.t(TAG).d("register");
        mEventBus.register(mPlaylistEditor);
        mEventBus.register(mClipsEditView);
//        mEventBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Logger.t(TAG).d("unregister");
        mEventBus.unregister(mPlaylistEditor);
        mEventBus.unregister(mClipsEditView);
//        mEventBus.unregister(this);
    }

    @Override
    public void finish() {
        MaterialDialog dialog = new MaterialDialog.Builder(EnhanceActivity.this)
            .content(R.string.discard_enhance_confirm)
            .positiveText(R.string.ok)
            .negativeText(R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    EnhanceActivity.super.finish();
                }
            }).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_ENHANCE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ArrayList<Clip> clips = data.getParcelableArrayListExtra(EXTRA_CLIPS_TO_APPEND);
                    Logger.t(TAG).d("append clips: " + clips.size());
                    mPlaylistEditor.add(clips);
                    if (getClipSet().getCount() > 0 && mViewAnimator.getDisplayedChild() == ACTION_ADD_VIDEO) {
                        btnGauge.setEnabled(true);
                        btnMusic.setEnabled(true);
                        configureActionUI(ACTION_NONE, false);
                    }

                }
                break;
            case REQUEST_CODE_ADD_MUSIC:
                Logger.t(TAG).d("Resultcode: " + resultCode + " data: " + data);
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mMusicItem = MusicItem.fromBundle(data.getBundleExtra("music.item"));
//                    updateMusicUI();
                    btnMusic.setSelected(true);
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
        initViews();
        Intent intent = getIntent();
        int launchMode = intent.getIntExtra(EXTRA_LAUNCH_MODE, 0);
        if (launchMode == LAUNCH_MODE_PLAYLIST) {
            mPlaylistId = getIntent().getIntExtra(EXTRA_PLAYLIST_ID, -1);
            mPlaylistEditor = new PlayListEditor(mVdbRequestQueue, mPlaylistId);
            mPlaylistEditor.reconstruct();
            embedVideoPlayFragment();
            configEnhanceView();
        } else {
            mPlaylistEditor = new PlayListEditor(mVdbRequestQueue, PLAYLIST_INDEX);
            ArrayList<Clip> clipArrayList = intent.getParcelableArrayListExtra(EXTRA_CLIP_LIST);
            mPlaylistEditor.build(clipArrayList, new PlayListEditor.OnBuildCompleteListener() {

                @Override
                public void onBuildComplete(ClipSet clipSet) {
                    embedVideoPlayFragment();
                    configEnhanceView();
                }
            });
        }

    }

    private void initViews() {
        setContentView(R.layout.activity_enhance2);
        setupToolbar();
        mClipsEditView.setVisibility(View.VISIBLE);
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.enhance);
        getToolbar().getMenu().clear();
        getToolbar().inflateMenu(R.menu.menu_enhance);
        if (getClipSet() != null && getClipSet().getCount() == 0) {
            getToolbar().getMenu().removeItem(R.id.menu_to_share);
            getToolbar().getMenu().removeItem(R.id.menu_to_download);
        }
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_to_share:
                        toShare();
                        break;
                    case R.id.menu_to_download:
                        new MaterialDialog.Builder(EnhanceActivity.this)
                            .title(R.string.download)
                            .items(R.array.download_resolution)
                            .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
//                            showToast(which + ": " + text);
                                    return true; // allow selection
                                }
                            })
                            .positiveText(R.string.download)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    doDownloadClips(dialog.getSelectedIndex());
                                }
                            })
                            .show();
                        break;
                }
                return true;
            }

        });

    }

    private void toShare() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            AuthorizeActivity.launch(EnhanceActivity.this);
            return;
        }else if (!SessionManager.checkUserVerified(EnhanceActivity.this)) {
            return;
        } else if (getClipSet().getCount() == 0) {
            MaterialDialog dialog = new MaterialDialog.Builder(EnhanceActivity.this)
                .content(R.string.no_clip_selected)
                .positiveText(R.string.ok)
                .show();
        } else {
            ShareActivity.launch(EnhanceActivity.this, mPlaylistEditor.getPlaylistId(), getAudioID());
//            finish();
        }
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



    private void doDownloadClips(final int selectIndex) {
        Clip.ID cid = new Clip.ID(PLAYLIST_INDEX, 0, null); // TODO
        DownloadUrlRequest request = new DownloadUrlRequest(cid, 0, getClipSet().getTotalLengthMs(), new VdbResponse
            .Listener<ClipDownloadInfo>() {
            @Override
            public void onResponse(ClipDownloadInfo response) {
                Logger.t(TAG).d("on response:!!!!: " + response.main.url);
                Logger.t(TAG).d("on response: " + response.sub.url);
//                Logger.t(TAG).d("on response:!!! poster data size: " + response.posterData.length);


                ClipDownloadInfo.StreamDownloadInfo downloadInfo;
                if (selectIndex == 0) {
                    downloadInfo = response.sub;
                } else {
                    downloadInfo = response.main;
                }

                JobManager jobManager = BgJobManager.getManager();
                DownloadJob job = new DownloadJob(getClipSet().getClip(0).streams[0], downloadInfo);
                jobManager.addJobInBackground(job);

                //startDownload(response, 0, clipSegment.getClip().streams[0]);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);

    }

    private void configEnhanceView() {
//        mClipsEditView.setClipIndex(mPlaylistEditor.getPlaylistId());
        mClipsEditView.setPlayListEditor(mPlaylistEditor);
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
            public void onClipRemoved(int clipCount) {

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


    public ClipSet getClipSet() {
        if (mPlaylistEditor != null) {
            return ClipSetManager.getManager().getClipSet(mPlaylistEditor.getPlaylistId());
        }
        return null;
    }

    private int getAudioID() {
        if (mMusicItem != null) {
            return mMusicItem.id;
        } else {
            return -1;
        }
    }
}
