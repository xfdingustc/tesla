package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.transition.TransitionManager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.birbit.android.jobqueue.JobManager;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.download.DownloadJob;
import com.waylens.hachi.bgjob.download.event.DownloadEvent;
import com.waylens.hachi.bgjob.upload.event.UploadEvent;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipDeleteRequest;
import com.waylens.hachi.snipe.toolbox.ClipExtentGetRequest;
import com.waylens.hachi.snipe.toolbox.ClipExtentUpdateRequest;
import com.waylens.hachi.snipe.toolbox.DownloadUrlRequest;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.authorization.LoginActivity;
import com.waylens.hachi.ui.clips.cliptrimmer.VideoTrimmer;
import com.waylens.hachi.ui.clips.player.ClipPlayFragment;
import com.waylens.hachi.ui.clips.player.ClipUrlProvider;
import com.waylens.hachi.ui.clips.player.UrlProvider;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipDownloadInfo;
import com.waylens.hachi.vdb.ClipExtent;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;


public class EnhancementActivity extends BaseActivity implements FragmentNavigator,
    android.support.v7.widget.Toolbar.OnMenuItemClickListener,
    ClipPlayFragment.ClipPlayFragmentContainer {
    private static final String TAG = EnhancementActivity.class.getSimpleName();
    public static final int REQUEST_CODE_SIGN_UP_FROM_ENHANCE = 200;

    public static final String EXTRA_CLIPS_TO_ENHANCE = "extra.clips.to.enhance";
    public static final String EXTRA_CLIPS_TO_APPEND = "extra.clips.to.append";
    public static final String EXTRA_LAUNCH_MODE = "extra.launch.mode";

    public static final int LAUNCH_MODE_QUICK_VIEW = 0;
    public static final int LAUNCH_MODE_ENHANCE = 1;
    public static final int LAUNCH_MODE_SHARE = 2;
    public static final int LAUNCH_MODE_MODIFY = 3;

    public static final int PLAYLIST_INDEX = 0x100;

    @BindView(R.id.player_fragment_content)
    ViewGroup mPlayerContainer;

    @BindView(R.id.clipTrimmer)
    VideoTrimmer mClipTrimmer;

    private ClipPlayFragment mClipPlayFragment;

    private int mLaunchMode = -1;


    EnhanceFragment mEnhanceFragment;
    ShareFragment mShareFragment;

    int mOriginalTopMargin;
    int mOriginalHeight;

    private SharableClip mSharableClip;

    private MaterialDialog mDownloadDialog;
    private MaterialDialog mUploadDialog;

    private EventBus mEventBus = EventBus.getDefault();

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
                    mDownloadDialog.getProgressBar().setProgress(progress);
                }
                break;
            case DownloadEvent.DOWNLOAD_WHAT_FINISHED:
                if (mDownloadDialog != null) {
                    mDownloadDialog.dismiss();
                }
                MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .content("Stream has been download into " + (String) event.getExtra())
                    .show();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventUpload(UploadEvent event) {
        switch (event.getWhat()) {
            case UploadEvent.UPLOAD_WHAT_START:
                mUploadDialog = new MaterialDialog.Builder(this)
                    .title(R.string.upload)
                    .contentGravity(GravityEnum.CENTER)
                    .progress(false, 100, true)
                    .show();
                mUploadDialog.setCanceledOnTouchOutside(false);
                break;
            case UploadEvent.UPLOAD_WHAT_PROGRESS:
                if (mUploadDialog != null) {
                    int progress = event.getExtra();
                    mUploadDialog.getProgressBar().setProgress(progress);
                }
                break;
            case UploadEvent.UPLOAD_WHAT_FINISHED:
                if (mUploadDialog != null) {
                    mUploadDialog.dismiss();
                }
                MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .content("Uploading finished")
                    .show();
                break;
        }
    }


    public static void launch(Activity activity, ArrayList<Clip> clipList, int launchMode) {
        Intent intent = new Intent(activity, EnhancementActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_CLIPS_TO_ENHANCE, clipList);
        intent.putExtra(EXTRA_LAUNCH_MODE, launchMode);
        activity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mEventBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mEventBus.unregister(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mPlayerContainer.getLayoutParams();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getToolbar().setVisibility(View.GONE);
            layoutParams.topMargin = 0;
            layoutParams.width = ViewGroup.MarginLayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.MarginLayoutParams.MATCH_PARENT;
            mPlayerContainer.setLayoutParams(layoutParams);
        } else {
            getToolbar().setVisibility(View.VISIBLE);
            layoutParams.width = ViewGroup.MarginLayoutParams.MATCH_PARENT;
            layoutParams.height = mOriginalHeight;
            if (mLaunchMode == LAUNCH_MODE_QUICK_VIEW) {
                layoutParams.topMargin = mOriginalTopMargin;
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        ArrayList<Clip> clipList = getIntent().getParcelableArrayListExtra(EXTRA_CLIPS_TO_ENHANCE);
        ClipSet clipSet = new ClipSet(Clip.TYPE_TEMP);

        for (Clip clip : clipList) {
            clipSet.addClip(clip);
        }

//        Logger.t(TAG).d("clip set size: " + clipSet.getCount() + " clip: " + clipSet.getClip(0).toString());

        if (!checkIfResolutionUnity(clipList)) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content(R.string.resolution_not_correct)
                .positiveText(android.R.string.ok)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        finish();
                    }
                })
                .build();
            dialog.show();
        }

        ClipSetManager.getManager().updateClipSet(ClipSetManager.CLIP_SET_TYPE_BOOKMARK, clipSet);

        ClipSetManager.getManager().updateClipSet(ClipSetManager.CLIP_SET_TYPE_ENHANCE, clipSet);

        Clip clip = getBookmarkClipSet().getClip(0);
        mSharableClip = new SharableClip(clip);
        Logger.t(TAG).d("buffered cid: " + mSharableClip.bufferedCid);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_enhance);

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mPlayerContainer.getLayoutParams();
        mOriginalTopMargin = layoutParams.topMargin;
        mOriginalHeight = layoutParams.height;

        //doBuildPlaylist();
        embedVideoPlayFragment();

        int launchMode = getIntent().getIntExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_QUICK_VIEW);
        switchToMode(launchMode);
    }



    private boolean checkIfResolutionUnity(List<Clip> clipList) {
        if (clipList.size() <= 1) {
            return true;
        }

        int firstClipWidth = clipList.get(0).streams[0].video_width;
        int firstClipHeight = clipList.get(0).streams[0].video_height;

        for (int i = 1; i < clipList.size(); i++) {
            Clip clip = clipList.get(i);
            if (clip.streams[0].video_width != firstClipWidth || clip.streams[0].video_height != firstClipHeight) {
                return false;
            }
        }

        return true;
    }

    private void switchToMode(int launchMode) {
//        Logger.t(TAG).d("switch to mode: " + launchMode);
        if (mLaunchMode == launchMode) {
            return;
        }
        mLaunchMode = launchMode;
        switch (mLaunchMode) {
            case LAUNCH_MODE_QUICK_VIEW:
                adjustPlayerPosition(true);
                mClipTrimmer.setVisibility(View.GONE);
                break;
            case LAUNCH_MODE_ENHANCE:
                mClipTrimmer.setVisibility(View.GONE);
                adjustPlayerPosition(false);
                if (mEnhanceFragment == null) {
                    mEnhanceFragment = new EnhanceFragment();
                }
                switchFragment(mShareFragment, mEnhanceFragment);
                break;
            case LAUNCH_MODE_SHARE:
                mClipTrimmer.setVisibility(View.GONE);
                if (!SessionManager.getInstance().isLoggedIn()) {
                    LoginActivity.launchForResult(this, REQUEST_CODE_SIGN_UP_FROM_ENHANCE);
                    return;
                } else {
                    adjustPlayerPosition(false);
                    if (mShareFragment == null) {
                        ArrayList<Clip> clipList = getClipSet().getClipList();
                        mShareFragment = ShareFragment.newInstance(clipList);
                    }
                    switchFragment(mEnhanceFragment, mShareFragment);
                }
                break;
            case LAUNCH_MODE_MODIFY:
                mClipTrimmer.setVisibility(View.VISIBLE);

                doGetClipExtension();
                break;
        }
        setupToolbarImpl();
    }

    void adjustPlayerPosition(boolean isQuickView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            TransitionManager.beginDelayedTransition(mPlayerContainer);
        }
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mPlayerContainer.getLayoutParams();
        if (isQuickView) {
            layoutParams.topMargin = ((ViewGroup.MarginLayoutParams) mPlayerContainer.getLayoutParams()).topMargin;
        } else {
            layoutParams.topMargin = 0;
        }
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setNavigationIcon(R.drawable.navbar_close);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLaunchMode == LAUNCH_MODE_ENHANCE) {
                    MaterialDialog dialog = new MaterialDialog.Builder(EnhancementActivity.this)
                        .title(R.string.discard_the_enhance)
                        .content(R.string.discard_enhance_confirm)
                        .positiveText(R.string.discard)
                        .negativeText(android.R.string.cancel)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                finish();
                            }
                        })
                        .build();
                    dialog.show();
                } else {
                    finish();
                }
            }
        });
        getToolbar().setOnMenuItemClickListener(this);
    }

    void setupToolbarImpl() {
        getToolbar().getMenu().clear();
        switch (mLaunchMode) {
            case LAUNCH_MODE_QUICK_VIEW:
                getToolbar().setTitle("");
                getToolbar().inflateMenu(R.menu.menu_clip_play_fragment);
                break;
            case LAUNCH_MODE_ENHANCE:
                getToolbar().setTitle(R.string.enhance);
                getToolbar().inflateMenu(R.menu.menu_enhance);
                break;
            case LAUNCH_MODE_SHARE:
                getToolbar().setTitle(R.string.share);
                getToolbar().inflateMenu(R.menu.menu_share);
                break;
            case LAUNCH_MODE_MODIFY:
                getToolbar().inflateMenu(R.menu.menu_clip_modify);
                break;
        }
    }

    private void embedVideoPlayFragment() {

        Clip clip = getClipSet().getClip(0);
        UrlProvider vdtUriProvider = new ClipUrlProvider(mVdbRequestQueue, clip.cid, clip.getStartTimeMs(), clip.getDurationMs());

        mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, ClipSetManager.CLIP_SET_TYPE_ENHANCE, vdtUriProvider, ClipPlayFragment.ClipMode.MULTI);
//        mClipPlayFragment.setShowsDialog(false);
        getFragmentManager().beginTransaction().replace(R.id.player_fragment_content, mClipPlayFragment).commit();
    }

    @Override
    public boolean onInterceptBackPressed() {
//        if (VideoPlayFragment.fullScreenPlayer != null) {
//            VideoPlayFragment.fullScreenPlayer.setFullScreen(false);
//            return true;
//        }
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_to_enhance:
                switchToMode(LAUNCH_MODE_ENHANCE);
                break;
            case R.id.menu_to_share:
                switchToMode(LAUNCH_MODE_SHARE);
                break;
            case R.id.menu_to_modify:
                switchToMode(LAUNCH_MODE_MODIFY);
                //TODO
                break;
            case R.id.menu_to_delete:
                doDeleteClip();
                break;
            case R.id.menu_to_download:
                //TODO
                new MaterialDialog.Builder(this)
                    .title(R.string.download)
                    .items(R.array.download_resolution)
                    .itemsCallbackSingleChoice(2, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
//                            showToast(which + ": " + text);
                            return true; // allow selection
                        }
                    })
                    .positiveText(R.string.download)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                            doDownloadClips(dialog.getSelectedIndex());
                        }
                    })
                    .show();

                break;
            case R.id.save:
                doSaveClipTrimInfo();
                break;
        }
        return true;
    }


    private void initClipTrimmer() {
        int defaultHeight = ViewUtils.dp2px(64);

        mClipTrimmer.setBackgroundClip(mVdbImageLoader, mSharableClip.clip, defaultHeight);
        mClipTrimmer.setEditing(true);
        mClipTrimmer.setInitRangeValues(mSharableClip.minExtensibleValue, mSharableClip.maxExtensibleValue);
        mClipTrimmer.setLeftValue(mSharableClip.selectedStartValue);
        mClipTrimmer.setRightValue(mSharableClip.selectedEndValue);
        mClipTrimmer.setOnChangeListener(new VideoTrimmer.OnTrimmerChangeListener() {
            @Override
            public void onStartTrackingTouch(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag) {
            }

            @Override
            public void onProgressChanged(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag, long start, long end, long progress) {
                long currentTimeMs = 0;
                if (flag == VideoTrimmer.DraggingFlag.LEFT) {
                    currentTimeMs = start;
                    mSharableClip.selectedStartValue = start;
                } else if (flag == VideoTrimmer.DraggingFlag.RIGHT) {
                    currentTimeMs = end;
                    mSharableClip.selectedEndValue = end;
                } else {
                    currentTimeMs = progress;
                }
                mClipPlayFragment.showClipPosThumbnail(mSharableClip.clip, currentTimeMs);
            }

            @Override
            public void onStopTrackingTouch(VideoTrimmer trimmer) {
                UrlProvider vdtUriProvider = new ClipUrlProvider(mVdbRequestQueue,
                    mSharableClip.bufferedCid, mSharableClip.selectedStartValue,
                    mSharableClip.getSelectedLength());
                mClipPlayFragment.setUrlProvider(vdtUriProvider);
            }
        });


    }


    private void doSaveClipTrimInfo() {
        if (mSharableClip.selectedStartValue == mSharableClip.clip.getStartTimeMs() &&
            mSharableClip.getSelectedLength() == mSharableClip.clip.getDurationMs()) {
            return;
        }

        Logger.t(TAG).d("LeftValue: " + mClipTrimmer.getLeftValue() + " RightValue: " + mClipTrimmer.getRightValue());
        mVdbRequestQueue.add(new ClipExtentUpdateRequest(mSharableClip.clip.cid,
            mClipTrimmer.getLeftValue(),
            mClipTrimmer.getRightValue(),
            new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
//                    hasUpdated = true;
//                    Snackbar.make(mRootView, R.string.bookmark_update_successful, Snackbar.LENGTH_SHORT).show();
                    switchToMode(LAUNCH_MODE_QUICK_VIEW);

                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
//                    Snackbar.make(mRootView, R.string.bookmark_update_error, Snackbar.LENGTH_SHORT).show();
                }
            }
        ));
    }


    private void doDownloadClips(final int selectIndex) {
//        DownloadUrlRequest request =
//        ClipSegment clipSegment = new ClipSegment(getClipSet().getClip(0));
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

    private void doGetClipExtension() {
        if (mSharableClip == null) {
            return;
        }
        mVdbRequestQueue.add(new ClipExtentGetRequest(mSharableClip.clip, new VdbResponse.Listener<ClipExtent>() {
            @Override
            public void onResponse(ClipExtent clipExtent) {
                if (clipExtent != null) {
//                    Logger.t(TAG).d("buffered cid: " + mSharableClip.bufferedCid);
                    mSharableClip.calculateExtension(clipExtent);
                    Logger.t(TAG).d("buffered cid: " + mSharableClip.bufferedCid);
                    Clip clip = getBookmarkClipSet().getClip(0);
                    UrlProvider vdtUriProvider = new ClipUrlProvider(mVdbRequestQueue,
                        mSharableClip.bufferedCid, mSharableClip.selectedStartValue,
                        mSharableClip.getSelectedLength());
                    mClipPlayFragment.setUrlProvider(vdtUriProvider);
                    initClipTrimmer();
                }
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Logger.t(TAG).d(error.toString());
            }
        }));
    }

    private void doDeleteClip() {
        ClipSet clipSet = ClipSetManager.getManager().getClipSet(ClipSetManager.CLIP_SET_TYPE_BOOKMARK);
        Clip clip = clipSet.getClip(0);
        Logger.t(TAG).d("Delete clip: " + clip.cid.toString());
        ClipDeleteRequest request = new ClipDeleteRequest(clip.cid, new VdbResponse.Listener<Integer>() {
            @Override
            public void onResponse(Integer response) {
                finish();
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_UP_FROM_ENHANCE:
                if (resultCode == RESULT_OK) {
                    switchToMode(LAUNCH_MODE_SHARE);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    void switchFragment(Fragment currentFragment, Fragment newFragment) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (currentFragment != null) {
            ft.hide(currentFragment);
        }
        if (newFragment.isAdded()) {
            ft.show(newFragment);
        } else {
            ft.add(R.id.enhance_fragment_content, newFragment);
        }
        ft.commit();
    }

    @Override
    public ClipPlayFragment getClipPlayFragment() {
        return mClipPlayFragment;
    }

    public int getAudioID() {
        if (mEnhanceFragment != null) {
            return mEnhanceFragment.getAudioID();
        } else {
            return EnhanceFragment.DEFAULT_AUDIO_ID;
        }
    }

    public JSONObject getGaugeSettings() {
        if (mEnhanceFragment != null) {
            return mEnhanceFragment.getGaugeSettings();
        } else {
            return null;
        }
    }

    private ClipSet getClipSet() {
        return ClipSetManager.getManager().getClipSet(ClipSetManager.CLIP_SET_TYPE_ENHANCE);
    }

    private ClipSet getBookmarkClipSet() {
        return ClipSetManager.getManager().getClipSet(ClipSetManager.CLIP_SET_TYPE_BOOKMARK);
    }
}
