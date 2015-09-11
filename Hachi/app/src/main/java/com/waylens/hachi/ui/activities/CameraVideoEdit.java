package com.waylens.hachi.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.transee.common.BitmapView;
import com.transee.common.DateTime;
import com.transee.common.GPSPath;
import com.transee.common.GPSRawData;
import com.transee.common.Utils;
import com.transee.common.VideoView;
import com.transee.common.ViewAnimation;
import com.transee.common.ViewAnimation.AlphaAnimation;
import com.transee.common.ViewAnimation.AlphaTranslateAnimation;
import com.transee.common.ViewAnimation.Animation;
import com.transee.common.ViewAnimation.TranslateAnimation;
import com.transee.common.ViewHolder;
import com.transee.vdb.ImageDecoder;
import com.transee.vdb.Playlist;
import com.transee.vdb.SlideView;
import com.transee.vdb.Vdb;
import com.transee.vdb.VdbClient;
import com.transee.vdb.VdbClient.PlaylistPlaybackUrl;
import com.transee.viditcam.actions.SelectMapProvider;
import com.transee.viditcam.app.CameraVideoEditPref;
import com.transee.viditcam.app.VdbEditor;
import com.transee.viditcam.app.ViditImageButton;
import com.transee.viditcam.app.ViditImageButton.OnStateChangedListener;
import com.transee.viditcam.app.comp.MapProvider;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.comp.VdbEdit;
import com.waylens.hachi.comp.VdbImageVideo;
import com.waylens.hachi.comp.VdbMap;
import com.waylens.hachi.comp.VdbMapAmap;
import com.waylens.hachi.comp.VdbMapGoogle;
import com.waylens.hachi.comp.VdbPlayback;
import com.waylens.hachi.ui.services.DownloadService;
import com.waylens.hachi.ui.services.DownloadService.DownloadInfo;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.DownloadInfoEx;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawData;
import com.waylens.hachi.vdb.RawDataBlock;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

abstract public class CameraVideoEdit {

    static final boolean DEBUG = false;
    static final String TAG = "CameraVideoEdit";

    public static final int JPEG_QUALITY = 90;
    public static final float DEFAULT_ZOOM = 4.0f;
    public static final int[] SPEED = new int[]{5, 10, 15, 20};

    abstract public void onBeginEdit();

    abstract public void onEndEdit();

    abstract public void requestDeleteClip(Clip.ID cid);

    abstract public void requestClearPlaylist(int plistId);

    abstract public Rect getClipThumbnailRect(Clip.ID cid, View other);

    abstract public Rect getPlaylistThumbnailRect(int plistId, View other);

    static final int EDIT_ANIM_DURATION = 300;
    static final int SLIDE_ANIM_DURATION = 100;
    static final int MAP_ANIM_DURATION = 250;
    static final int BUTTON_HINT_ANIM_START = 250;
    static final int BUTTON_HINT_ANIM_DURATION = 500;
    static final int TEXT_HINT_ANIM_START = 1000;
    static final int TEXT_HINT_ANIM_DURATION = 1500;
    static final int ANIM_STEP = 10;

    private final BaseActivity mActivity;
    private final ImageDecoder mDecoder;
    private final VdbEditor mEditor;

    private boolean mbEditing;
    private View mVideoEditLayout;

    private View mVideoControl;
    private View mSlideControl;
    private View mMapControl;

    private ViewHolder mVideoHolder;
    private ViewHolder mSlideHolder;

    private View mToolbar;
    private ViditImageButton mReturnButton;
    private ViditImageButton mPlayBackwardButton;
    private ViditImageButton mPlayForwardButton;
    private ViditImageButton mShareButton;

    private ViewAnimation mEditAnimation;
    private ViewAnimation mSlideAnimation;
    private ViewAnimation mMapAnimation;
    private ViewAnimation mHintAnimation;

    private boolean mSlideControlVisible;
    private boolean mMapVisible;

    private final VdbPlayback mVdbPlayback;
    private final VdbImageVideo mVdbImageVideo;
    private final VdbEdit mVdbEdit;
    private VdbMap mVdbMap;

    private View mEditControls;
    private View mEditInfoView;
    private View mEditOverlay;
    private Button mShowMapButton;

    private TextView mPosText;
    private TextView mSelectionText;
    private TextView mDownloadInfoText;
    private TextView mHintText;

    private boolean mbLayoutChanged;
    private boolean mbVideoPlaybackPosChanging;
    private boolean mbPlaybackEnding;

    private final CameraVideoEditPref mPref = new CameraVideoEditPref();

    private Hachi.DownloadCallback mDownloadCallback = new Hachi.DownloadCallback() {
        @Override
        public void onDownloadInfo(DownloadInfo downloadInfo) {
            CameraVideoEdit.this.onDownloadInfo(downloadInfo);
        }
    };

    @SuppressLint("InflateParams")
    public CameraVideoEdit(BaseActivity activity, Vdb vdb, ImageDecoder decoder) {

        mPref.load(activity);

        mActivity = activity;
        mDecoder = decoder;
        mEditor = new VdbEditor(vdb, mPref.playLowBitrateStream);

        LayoutInflater lf = activity.getLayoutInflater();

        // IV controls
        mVideoControl = lf.inflate(R.layout.group_clip_video, null);
        mVideoControl.setLayoutParams(new FrameLayout.LayoutParams(0, 0));

        // slide controls
        mSlideControl = lf.inflate(R.layout.group_clip_slide, null);
        mSlideControl.setLayoutParams(new FrameLayout.LayoutParams(0, 0));

        // map controls
        mMapControl = lf.inflate(R.layout.group_clip_map, null);
        mMapControl.setLayoutParams(new FrameLayout.LayoutParams(32, 32));

        // components
        mVdbPlayback = new MyVdbPlayback(activity);
        mVdbImageVideo = new MyVdbImageVideo(activity, mEditor, mPref.showAcc, mVdbPlayback);
        mVdbEdit = new MyVdbEdit(activity, mVdbImageVideo.getSlideView(), mPref.showButtonHint);
        createVdbMap();
    }

    private VdbMap createVdbMap() {
        if (mVdbMap == null) {
            switch (mPref.mapProvider) {
                case MapProvider.MAP_BAIDU:
                    break;
                case MapProvider.MAP_AMAP:
                    mVdbMap = new VdbMapAmap(mActivity, (RelativeLayout) mMapControl, mEditor);
                    break;
                case MapProvider.MAP_GOOGLE:
                    VdbMapGoogle.initialize(mActivity);
                    mVdbMap = new MyVdbMapGoogle(mActivity, (RelativeLayout) mMapControl, mEditor);
                    break;
                default:
                    break;
            }
        }
        return mVdbMap;
    }

    private void destroyVdbMap() {
        if (mVdbMap != null) {
            mVdbMap.onDestroy();
            mVdbMap = null;
        }
    }

    // API
    public final boolean isEditing() {
        return mbEditing;
    }

    // API
    public final void onCreateActivity(Bundle savedInstanceState) {
        if (mVdbMap != null) {
            mVdbMap.onCreate(savedInstanceState);
        }
    }

    // API
    public final void onDestroyActivity() {
        destroyVdbMap();
    }

    // API
    public final void onStartActivity() {
        //mActivity.thisApp.attachDownloadAdmin(mDownloadCallback);
    }

    // API
    public final void onStopActivity() {
        //mActivity.thisApp.detachDownloadAdmin(mDownloadCallback);
        mVdbImageVideo.onStopActivity();
        if (mEditor.mbVideoOnly) {
            mVdbPlayback.pausePlayback();
        } else {
            stopPlayback(false);
        }
    }

    // API
    public final void onPauseActivity() {
        if (mVdbMap != null) {
            mVdbMap.onPause();
        }
    }

    // API
    public final void onResumeActivity() {
        if (mVdbMap != null) {
            mVdbMap.onResume();
        }
    }

    // API
    public void onSaveInstanceState(Bundle outState) {
        if (mVdbMap != null) {
            mVdbMap.onSaveInstanceState(outState);
        }
    }

    // API
    public void onLowMemory() {
        if (mVdbMap != null) {
            mVdbMap.onLowMemory();
        }
    }

    private void onDownloadInfo(DownloadInfo downloadInfo) {
        if (mEditor.mVdb.isLocal()) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "state " + downloadInfo.state + ", percent " + downloadInfo.percent);
        }
        switch (downloadInfo.state) {
            case DownloadService.DOWNLOAD_STATE_IDLE:
                mDownloadInfoText.setVisibility(View.GONE);
                break;
            case DownloadService.DOWNLOAD_STATE_FINISHED:
                setDownloadInfo(100, downloadInfo);
                mDownloadInfoText.setVisibility(View.GONE);
                break;
            case DownloadService.DOWNLOAD_STATE_RUNNING:
                setDownloadInfo(downloadInfo.percent, downloadInfo);
                break;
            case DownloadService.DOWNLOAD_STATE_ERROR:
                // TODO - show error msg
                break;
        }
    }

    private void setDownloadInfo(int percent, DownloadInfo downloadInfo) {
        int remain = downloadInfo.list.size();
        if (percent == 0 && downloadInfo.item == null && remain == 0) {
            mDownloadInfoText.setVisibility(View.GONE);
            return;
        }
        String text = Integer.toString(percent) + "%";
        if (remain > 0) {
            String fmt = mActivity.getResources().getString(R.string.lable_download_remain);
            text += " " + String.format(Locale.US, fmt, remain);
        }
        if (DEBUG) {
            Log.d(TAG, text);
        }
        mDownloadInfoText.setText(text);
        mDownloadInfoText.setVisibility(View.VISIBLE);
    }

    private void stopEditAnimation() {
        if (mEditAnimation != null) {
            mEditAnimation.stopAnimation();
            mEditAnimation = null;
        }
    }

    private void stopSlideAnimation() {
        if (mSlideAnimation != null) {
            mSlideAnimation.stopAnimation();
            mSlideAnimation = null;
        }
    }

    private void stopMapAnimation() {
        if (mMapAnimation != null) {
            mMapAnimation.stopAnimation();
            mMapAnimation = null;
        }
    }

    private void stopHintAnimation() {
        if (mHintAnimation != null) {
            mHintAnimation.stopAnimation();
            mHintAnimation = null;
        }
    }

    private final boolean hasMap() {
        return mEditor.hasMap();
    }

    // API
    public final void onReleaseUI() {
        // stop animations
        stopEditAnimation();
        stopSlideAnimation();
        stopMapAnimation();
        stopHintAnimation();

        // stop playback
        if (!mActivity.isRotating()) {
            stopPlayback(false);
        }

        // remove views
        mVideoHolder.setView(null);
        mSlideHolder.setView(null);

        ViewGroup root = (ViewGroup) mVideoEditLayout.getParent();
        root.removeView(mVideoControl);
        root.removeView(mSlideControl);
        root.removeView(mMapControl);
    }

    // API
    public final void onInitUI() {
        mToolbar = mSlideControl.findViewById(R.id.clipToolbar);
        setToolbarButtonStateChangedListener(mToolbar);

        mReturnButton = (ViditImageButton) mToolbar.findViewById(R.id.btnReturn);
        mReturnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endEdit(false);
            }
        });

        mPlayBackwardButton = (ViditImageButton) mToolbar.findViewById(R.id.btnPlayBackward);
        mPlayBackwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playBackward();
            }
        });

        mPlayForwardButton = (ViditImageButton) mToolbar.findViewById(R.id.btnPlayForward);
        mPlayForwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playForward();
            }
        });

        mShareButton = (ViditImageButton) mToolbar.findViewById(R.id.btnShare);
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareVideo();
            }
        });

        if (mEditor.mVdb.isLocal()) {
            mShareButton.setVisibility(View.VISIBLE);
        } else {
            mShareButton.setVisibility(View.INVISIBLE);
        }

        mVideoEditLayout = mActivity.findViewById(R.id.cameraVideoEdit);
        mVideoEditLayout.addOnLayoutChangeListener(mOnVideoEditLayoutChange);

        mVideoHolder = (ViewHolder) mVideoEditLayout.findViewById(R.id.videoHolder);
        mSlideHolder = (ViewHolder) mVideoEditLayout.findViewById(R.id.slideHolder);

        mEditControls = mVideoEditLayout.findViewById(R.id.editControls);

        mEditInfoView = mEditControls.findViewById(R.id.editInfoView);
        mPosText = (TextView) mEditInfoView.findViewById(R.id.textView1);
        mSelectionText = (TextView) mEditInfoView.findViewById(R.id.textView2);
        mDownloadInfoText = (TextView) mEditInfoView.findViewById(R.id.textView3);

        mEditOverlay = mEditControls.findViewById(R.id.editOverlay);
        mHintText = (TextView) mEditOverlay.findViewById(R.id.textView4);

        mShowMapButton = (Button) mEditControls.findViewById(R.id.btnShowMap);
        mShowMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkMap()) {
                    mMapVisible = !mMapVisible;
                    startMapAnimation();
                }
            }
        });

        mVdbPlayback.onInitUI(mToolbar);
        mVdbEdit.onInitUI(mToolbar);

        // add views
        ViewGroup root = (ViewGroup) mVideoEditLayout.getParent();
        root.addView(mVideoControl);
        root.addView(mSlideControl);
        root.addView(mMapControl);

        mVideoHolder.setView(mVideoControl);
        mSlideHolder.setView(mSlideControl);
    }

    // API
    public final void onSetupUI() {
        if (mActivity.isLandscape()) {
            int margin = (int) Utils.dp2px(mActivity, 32);
            mMapControl.setPadding(margin, margin, margin, margin);
            mToolbar.setBackgroundColor(0x80ffffff);
            mMapControl.setAlpha(0.8f);
        } else {
            mMapControl.setPadding(0, 0, 0, 0);
            mToolbar.setBackgroundColor(Color.TRANSPARENT);
            mMapControl.setAlpha(1);
        }

        mVdbImageVideo.onSetupUI();
        mVdbPlayback.onSetupUI();

        if (mbEditing) {
            mSlideControlVisible = true;
            showViews();
            mEditControls.setVisibility(View.VISIBLE);
            mEditInfoView.setVisibility(View.VISIBLE);
            showMapButton();
            mVdbImageVideo.updatePosInfo(mPosText, mSelectionText);
        } else {
            mSlideControlVisible = false;
            hideMapButton();
        }

        mMapVisible = false;
        mMapControl.setVisibility(View.INVISIBLE);

        if (mActivity.mbRotating) {
            mbLayoutChanged = true;
        }
    }

    private void hideMapButton() {
        if (hasMap()) {
            mShowMapButton.setVisibility(View.INVISIBLE);
        } else {
            mShowMapButton.setVisibility(View.GONE);
        }
    }

    private void showMapButton() {
        if (hasMap()) {
            mShowMapButton.setVisibility(View.VISIBLE);
        }
    }

    private void restoreEditControlsLayout() {
        MarginLayoutParams lp = (MarginLayoutParams) mEditControls.getLayoutParams();
        lp.setMargins(0, 0, 0, 0);
        mEditControls.setLayoutParams(lp);
    }

    private void showViews() {
        mVideoControl.setVisibility(View.VISIBLE);
        mSlideControl.setVisibility(View.VISIBLE);
        mMapControl.setVisibility(View.VISIBLE);
        mVideoEditLayout.setVisibility(View.VISIBLE);
        mVideoEditLayout.bringToFront();
        restoreEditControlsLayout();
    }

    private void hideViews() {
        mVdbImageVideo.onHideView();
        mVideoControl.setVisibility(View.INVISIBLE);
        mSlideControl.setVisibility(View.INVISIBLE);
        mMapControl.setVisibility(View.INVISIBLE);
        mVideoEditLayout.setVisibility(View.INVISIBLE);
    }

    // API
    public final void editClip(Bitmap bitmap, Clip clip, int offset, int size) {
        mEditor.editClip(clip);
        if (clip.isLocal()) {
            mVdbEdit.disableSelect();
        } else {
            mVdbEdit.enableSelect();
        }
        mVdbImageVideo.editClip(bitmap, clip, offset, size);
        beginEdit();
    }

    // API
    public final void editPlaylist(Bitmap bitmap, Playlist playlist) {
        mEditor.editPlaylist(playlist, 0);
        mVdbEdit.disableSelect();
        mVdbImageVideo.editPlaylist(bitmap, playlist);
        beginEdit();
    }

    private void beginEdit() {
        mbEditing = true;
        mVdbEdit.initButtonState();
        startEditAnimation(false);
        doShowMap();
    }

    // API
    public final void endEdit(boolean bNoAnimation) {
        if (mbEditing) {
            doEndEdit(false, bNoAnimation);
            doHideMap();
        }
    }

    private void doShowMap() {
        if (mMapVisible) {
            if (mVdbMap != null) {
                mVdbMap.showMap();
            }
        } else {
            mMapControl.setVisibility(View.INVISIBLE);
        }
    }

    private void doHideMap() {
        if (!mMapVisible) {
            if (mVdbMap != null) {
                mVdbMap.hideMap();
            }
        }
    }

    private void doEndEdit(boolean bDelete, boolean bNoAnimation) {
        stopPlayback(false);
        mEditor.mVdb.getClient().stopImageDecoder();

        if (bDelete) {
            if (mEditor.isEditingClip()) {
                requestDeleteClip(mEditor.mClip.cid);
            } else if (mEditor.isEditingPlaylist()) {
                requestClearPlaylist(mEditor.mPlaylist.plistId);
            }
        }

        mEditor.mbDelete = bDelete;
        mbEditing = false;

        if (!bNoAnimation) {
            startEditAnimation(true);
        } else {
            onEndEditAnimation(true);
        }
    }

    private void layoutMapControl() {
        BitmapView ivView = mVdbImageVideo.getImageView();
        Rect rect = new Rect();

        if (mActivity.isLandscape()) {
            // map on top right of image view; on top of slide view
            rect.right = ivView.getWidth();
            rect.left = rect.right / 2;
            rect.top = 0;
            rect.bottom = ivView.getHeight() - mSlideControl.getHeight();
            mMapControl.layout(rect.left, rect.top, rect.right, rect.bottom);
        } else {
            // map + image view + slide view
            ivView.getIdealSize(rect);
            rect.bottom = mVideoEditLayout.getHeight() - rect.bottom - mSlideControl.getHeight();
            mMapControl.layout(rect.left, rect.top, rect.right, rect.bottom);
        }
    }

    private void selectMap(final VdbMapGoogle vdbMap) {
        SelectMapProvider action = new SelectMapProvider(mActivity) {
            @Override
            public void onMapProviderSelected(int mapProvider) {
                CameraVideoEdit.this.onMapProviderSelected(vdbMap, mapProvider);
            }
        };
        action.show();
    }

    private void onMapProviderSelected(VdbMapGoogle vdbMap, int mapProvider) {
        mPref.mapProvider = mapProvider;
        mPref.save(mActivity);
        if (mPref.mapProvider != MapProvider.MAP_GOOGLE) {
            if (vdbMap != null) {
                vdbMap.removeEmptyMap();
            }
            mVdbMap = null; // force create
            createVdbMap();
            if (vdbMap == null) {
                mMapVisible = !mMapVisible;
                startMapAnimation();
            }
        }
    }

    private boolean checkMap() {
        if (createVdbMap() != null)
            return true;
        selectMap(null);
        return false;
    }

    private Animation createMapAnimation() {
        TranslateAnimation anim = ViewAnimation.createTranslateAnimationP(mMapControl, mMapControl, 0, -100, 0, 0);
        anim.setAnimationType(TranslateAnimation.TYPE_ACCELERATED);
        return anim;
    }

    private Animation createSlideBarAnimation() {
        TranslateAnimation anim = ViewAnimation.createTranslateAnimationP(mSlideControl, mSlideHolder, 0, 100, 0, 0);
        anim.setAnimationType(TranslateAnimation.TYPE_ACCELERATED);
        return anim;
    }

    private void startMapAnimation() {

        stopMapAnimation();

        if (hasMap()) {
            mShowMapButton.setText(mMapVisible ? R.string.btn_hide_map : R.string.btn_show_map);
        }

        layoutMapControl();

        mMapAnimation = new ViewAnimation() {
            @Override
            public void onAnimationStart(ViewAnimation animation, boolean bReverse) {
                if (mMapVisible) {
                    mVdbImageVideo.updateAnimation(VdbImageVideo.SHOW_MAP);
                    doShowMap();
                    mMapControl.setVisibility(View.VISIBLE);
                }
                mVdbImageVideo.onStartAnimation();
            }

            @Override
            public void onAnimationDone(ViewAnimation animation, boolean bReverse) {
                if (!mMapVisible) {
                    mMapControl.setVisibility(View.INVISIBLE);
                    doHideMap();
                } else {
                    if (hasMap()) {
                        mEditor.requestMapTrack();
                    }
                }
                setVideoViewPosition(true);
                mVdbImageVideo.onStopAnimation();
            }

            @Override
            protected void onAnimationUpdated(ViewAnimation animation, boolean bReverse) {
                if (mActivity.isPortrait()) {
                    setVideoViewPosition(false);
                }
            }
        };

        mMapAnimation.addAnimation(mMapControl, createMapAnimation());

        mMapAnimation.startAnimation(MAP_ANIM_DURATION, ANIM_STEP, !mMapVisible);
    }

    private void onStartEditAnimation(boolean bReverse) {
        mVdbImageVideo.onStartAnimation();
        if (bReverse) {
            // start to exit edit
            onEndEdit();
            mEditInfoView.setVisibility(View.INVISIBLE);
            mHintText.setVisibility(View.INVISIBLE);
            hideMapButton();
        } else {
            // start to enter edit
            showViews();
        }
    }

    private void onEndEditAnimation(boolean bReverse) {
        mVdbImageVideo.onStopAnimation();
        mVdbImageVideo.getImageView().setUserRect(false, null, 255);
        if (bReverse) {
            // end exit edit
            hideViews();
            if (mEditor.mbDelete) {
                mEditor.mbDelete = false;
                mEditor.deleteCurrent();
            }
        } else {
            // end enter edit
            onBeginEdit();
            mEditInfoView.setVisibility(View.VISIBLE);
            if (hasMap()) {
                showMapButton();
                mEditor.requestMapTrack();
            }
            if (mEditor.mbVideoOnly) {
                mVdbPlayback.preparePlayback(true);
            } else if (mPref.autoFastBrowse) {
                // onToolbarButtonStateChanged(mPlayForwardButton, true);
                // onToolbarButtonStateChanged(mPlayForwardButton, false);
                playForward();
            }
        }
    }

    private void shareVideo() {
        Uri uri = mEditor.getClipUri();
        if (uri != null) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setType("video/mp4");
            String title = mActivity.getResources().getString(R.string.title_share_with);
            mActivity.startActivity(Intent.createChooser(intent, title));
        }
    }

    private int getBackwardSpeed() {
        int currSpeed = mVdbImageVideo.getSlideView().getScrollSpeed();
        for (int i = 0; i < SPEED.length; i++) {
            if (currSpeed > -SPEED[i])
                return -SPEED[i];
        }
        return -SPEED[0];
    }

    private void playBackward() {
        int speed = getBackwardSpeed();
        int scrollLengthMs = mEditor.getLengthMs() / (-speed);
        mVdbImageVideo.getSlideView().scrollToLeft(scrollLengthMs, speed);
    }

    private int getForwardSpeed() {
        int currSpeed = mVdbImageVideo.getSlideView().getScrollSpeed();
        for (int i = 0; i < SPEED.length; i++) {
            if (currSpeed < SPEED[i]) {
                return SPEED[i];
            }
        }
        return SPEED[0];
    }

    private void playForward() {
        int speed = getForwardSpeed();
        int scrollLengthMs = mEditor.getLengthMs() / speed;
        mVdbImageVideo.getSlideView().scrollToRight(scrollLengthMs, speed);
    }

    private final int genAnimationFlags() {
        int flags = mVdbPlayback.isIdle() ? VdbImageVideo.SHOW_ANIM : 0;
        if (mMapVisible) {
            flags |= VdbImageVideo.SHOW_MAP;
        }
        return flags;
    }

    private void stopPlayback(boolean bScroll) {
        if (!bScroll) {
            mVdbImageVideo.getSlideView().cancelScroll();
        }
        mVdbImageVideo.beforeStopPlayback();
        if (mVdbPlayback.cancelPlayback()) {
            // refresh animation
            mVdbImageVideo.updateAnimation(genAnimationFlags());
        }
    }

    private final void showSlideControl(boolean bShow) {
        if (!mActivity.isLandscape())
            return;

        if (mSlideControlVisible == bShow)
            return;

        mSlideControlVisible = bShow;
        stopSlideAnimation();

        mSlideAnimation = new ViewAnimation() {
            @Override
            protected void onAnimationStart(ViewAnimation animation, boolean bReverse) {
                if (!bReverse) { // begin show
                    restoreEditControlsLayout();
                    mEditControls.setVisibility(View.VISIBLE);
                }
            }

            @Override
            protected void onAnimationDone(ViewAnimation animation, boolean bReverse) {
                if (bReverse) { // end hide
                    mEditControls.setVisibility(View.INVISIBLE);
                }
            }
        };

        // slide view, y +100 -> +0
        Animation anim = createSlideBarAnimation();
        mSlideAnimation.addAnimation(mSlideControl, anim);

        // edit controls
        anim = ViewAnimation
            .createTranslateAnimationD(mEditControls, mEditControls, 0, -mSlideHolder.getHeight(), 0, 0);
        mSlideAnimation.addAnimation(mEditControls, anim);

        // start animation
        mSlideAnimation.startAnimation(SLIDE_ANIM_DURATION, ANIM_STEP, !mSlideControlVisible);
    }

    // API - decide if we should decode the data. If yes, call decoder.decode()
    public final void decodeImage(ClipPos clipPos, byte[] data) {
        mVdbImageVideo.decodeImage(mDecoder, clipPos, data);
    }

    // API
    public void onBitmapDecoded(ClipPos clipPos, Bitmap bitmap) {
        mVdbImageVideo.onBitmapDecoded(clipPos, bitmap);
    }

    // API
    public final void onPlaybackUrl(PlaybackUrl playbackUrl) {
        if (DEBUG) {
            Log.d(TAG, "playbackUrl: " + playbackUrl.url);
        }
        Clip.StreamInfo streamInfo = mEditor.getClipStreamInfo(playbackUrl.cid, playbackUrl.stream);
        if (streamInfo != null) {
            mVdbPlayback.playUrl(streamInfo, playbackUrl.url, playbackUrl.realTimeMs, playbackUrl.lengthMs,
                playbackUrl.offsetMs, null, 0, mEditor.bMuteAudio);
        }
    }

    // API
    public final void onPlaybackUrlError() {
        if (DEBUG) {
            Log.d(TAG, "failed to get playback url");
        }
        mVdbPlayback.playbackUrlError();
    }

    // API
    public final void onPlaylistPlaybackUrl(PlaylistPlaybackUrl playbackUrl) {
        if (DEBUG) {
            Log.d(TAG, "playbackUrl: " + playbackUrl.url);
        }
        Log.e("test", "URL: " + playbackUrl.url);
        Clip.StreamInfo streamInfo = mEditor.getPlaylistStreamInfo(playbackUrl.listType, playbackUrl.stream);
        if (streamInfo != null) {
            mVdbPlayback.playUrl(streamInfo, playbackUrl.url, playbackUrl.playlistStartTimeMs, playbackUrl.lengthMs, 0,
                mEditor.audioFileName, playbackUrl.playlistStartTimeMs, mEditor.bMuteAudio);
        }
    }

    // API
    public final void onPlaylistPlaybackUrlError() {
        if (DEBUG) {
            Log.d(TAG, "failed to get playlist playback url");
        }
        mVdbPlayback.playbackUrlError();
    }

    // API
    public final void onMarkClipResult(int error) {
        mVdbEdit.onMarkClipResult(error);
    }

    // API
    public final void onInsertClipResult(int error) {
        mVdbEdit.onInsertClipResult(error);
    }

    // API
    public final void onClipChanged(boolean isLive, Clip clip, boolean bFinished) {
        if (mEditor.isEditingClip(clip)) {
            mVdbEdit.onClipChanged(clip, bFinished);
            // TODO
        }
    }

    // API
    public void onClipInserted(boolean isLive, Clip clip) {
        // TODO
    }

    // API
    public void onClipMoved(boolean isLive, Clip clip) {
        if (mEditor.isEditingPlaylist(clip.cid.type)) {
            mVdbImageVideo.onClipMoved();
        }
    }

    // API
    public final void onClipRemoved(Clip.ID cid) {
        if (mEditor.isEditingClip(cid)) {
            endEdit(false);
        } else if (mEditor.isEditingPlaylist(cid.type)) {
            if (mEditor.mPlaylist.isEmpty()) {
                endEdit(false);
            } else {
                mVdbImageVideo.onClipRemoved(cid);
            }
        }
    }

    // API
    public final void onPlaylistCleared(int plistId) {
        if (mEditor.isEditingPlaylist(plistId)) {
            endEdit(false);
        }
    }

    // API
    public void onRawDataResult(RawData rawDataResult) {
        if (mMapVisible) { // TODO
            for (int i = 0; i < rawDataResult.items.size(); i++) {
                RawData.RawDataItem item = rawDataResult.items.get(i);
                if (item.dataType == VdbClient.RAW_DATA_GPS) {
                    GPSRawData rawData = (GPSRawData) item.object;
                    mVdbMap.setGPSRawData(rawData);
                    break;
                }
            }
        }
    }

    // API
    public final void onRawDataBlock(RawDataBlock block) {
        mVdbImageVideo.onRawDataBlock(block);
    }

    // API
    public final void onDownloadRawDataBlock(RawDataBlock.DownloadRawDataBlock block) {
        mVdbEdit.onDownloadedRawData(block);
    }

    // API
    public void onDownloadUrlFailed() {
        mVdbEdit.onDownloadUrlFailed();
    }

    // API
    public void onDownloadUrlReady(DownloadInfoEx downloadInfo, boolean bFirstLoop) {
        mVdbEdit.onDownloadUrlReady(downloadInfo, bFirstLoop);
    }

    // API
    public void onGPSSegment(GPSPath.Segment segment) {
        if (mEditor.isEditing() && mVdbMap != null) {
            if (mEditor.isEditingClip(segment.mHeader.cid)) {
                mVdbMap.addSegment(segment);
            } else if (mEditor.isEditingPlaylist(segment.mHeader.cid.type)) {
                mVdbMap.addSegment(segment);
            }
        }
    }

    final private OnLayoutChangeListener mOnVideoEditLayoutChange = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop,
                                   int oldRight, int oldBottom) {
            CameraVideoEdit.this.onLayoutChange(v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom);
        }
    };

    private void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop,
                                int oldRight, int oldBottom) {
        BitmapView imageView = mVdbImageVideo.getImageView();
        VideoView videoView = mVdbPlayback.getVideoView();
        if (mActivity.isLandscape()) {
            imageView.setAnchorRect(0, 0, 0, 0, DEFAULT_ZOOM);
            videoView.setAnchorRect(0, 0, 0, 0, DEFAULT_ZOOM);
        } else {
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                setVideoViewPosition(true);
            }
        }
        if (mbLayoutChanged) {
            mbLayoutChanged = false;
            mVdbImageVideo.onLayoutChanged();
        }
    }

    private void setVideoViewPosition(boolean bSetupVideoView) {
        if (mActivity.isPortrait()) {
            BitmapView imageView = mVdbImageVideo.getImageView();
            VideoView videoView = mVdbPlayback.getVideoView();

            int leftMargin = 0;
            int rightMargin = 0;
            int topMargin = 0;
            mSlideControl.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            int bottomMargin = mSlideControl.getMeasuredHeight();
            if (mMapControl.getVisibility() == View.VISIBLE) {
                MarginLayoutParams lp = (MarginLayoutParams) mMapControl.getLayoutParams();
                topMargin += lp.height + lp.topMargin;
            }

            imageView.setAnchorRect(leftMargin, topMargin, rightMargin, bottomMargin, -1.0f);
            if (videoView.canAnimate() || bSetupVideoView) {
                videoView.setAnchorRect(leftMargin, topMargin, rightMargin, bottomMargin, -1.0f);
            }
        }

        mVdbImageVideo.updateProgressPos(false);
    }

    private void startEditAnimation(boolean bReverse) {

        stopEditAnimation();

        Rect rectFrom = null;
        BitmapView view = mVdbImageVideo.getImageView();
        if (mEditor.isEditingClip()) {
            rectFrom = getClipThumbnailRect(mEditor.mClip.cid, view);
        } else if (mEditor.isEditingPlaylist()) {
            rectFrom = getPlaylistThumbnailRect(mEditor.mPlaylist.plistId, view);
        }

        if (rectFrom == null) {
            onStartEditAnimation(bReverse);
            onEndEditAnimation(bReverse);
            return;
        }

        Rect rectTo = new Rect();
        if (!bReverse) {
            view.getBitmapRect(rectFrom.width(), rectFrom.height(), rectTo);
        } else {
            rectTo.set(view.getBitmapRect());
        }

        mEditAnimation = new ViewAnimation() {
            @Override
            public void onAnimationStart(ViewAnimation animation, boolean bReverse) {
                CameraVideoEdit.this.onStartEditAnimation(bReverse);
            }

            @Override
            public void onAnimationDone(ViewAnimation animation, boolean bReverse) {
                CameraVideoEdit.this.onEndEditAnimation(bReverse);
            }
        };

        TranslateAnimation translateAnim = ViewAnimation.createTranslateAnimation(rectFrom, rectTo);
        translateAnim.setAnimationType(TranslateAnimation.TYPE_ACCELERATED);
        AlphaAnimation alphaAnim = new ViewAnimation.AlphaAnimation(0, 255);
        AlphaTranslateAnimation anim = new AlphaTranslateAnimation(alphaAnim, translateAnim) {
            @Override
            public void updateView(Animation anim, View view, boolean bReverse) {
                AlphaTranslateAnimation thisAnim = (AlphaTranslateAnimation) anim;
                BitmapView v = (BitmapView) view;
                Rect rect = thisAnim.mTranslateAnimation.mRect;
                int alpha = thisAnim.mAlphaAnimation.mAlpha;
                v.setUserRect(true, rect, alpha);
                v.invalidate();
            }
        };
        mEditAnimation.addAnimation(view, anim);

        if (mMapVisible) {
            layoutMapControl();
            mEditAnimation.addAnimation(mMapControl, createMapAnimation());
        }

        if (!bReverse || mSlideControlVisible) {
            mSlideControlVisible = true;
            mEditAnimation.addAnimation(mSlideControl, createSlideBarAnimation());
        }

        mEditAnimation.startAnimation(EDIT_ANIM_DURATION, ANIM_STEP, bReverse);
    }

    private int onPlaybackPosChanged(long startTimeMs, int posMs) {
        int pos = mVdbImageVideo.onPlaybackPosChanged(startTimeMs, posMs);
        if (mMapVisible) {
            mVdbImageVideo.updateAnimation(VdbImageVideo.SHOW_MAP);
        }
        return pos;
    }

    private void setToolbarButtonStateChangedListener(View toolbar) {
        ViewGroup group = (ViewGroup) toolbar;
        int n = group.getChildCount();
        for (int i = 0; i < n; i++) {
            View view = group.getChildAt(i);
            if (view instanceof ViditImageButton) {
                ViditImageButton button = (ViditImageButton) view;
                button.setOnStateChangedListener(mOnButtonStateChanged);
            } else if (view instanceof ViewGroup) {
                setToolbarButtonStateChangedListener(view);
            }
        }
    }

    private final OnStateChangedListener mOnButtonStateChanged = new OnStateChangedListener() {
        @Override
        public void onStateChanged(ViditImageButton button, boolean bPressed) {
            onToolbarButtonStateChanged(button, bPressed);
        }
    };

    private void onToolbarButtonStateChanged(ViditImageButton button, boolean bPressed) {
        if (!mPref.showButtonHint) {
            return;
        }
        stopHintAnimation();
        if (bPressed) {
            int resId = -1;
            String text = null;
            switch (button.getId()) {
                case R.id.btnReturn:
                    resId = R.string.hint_edit_return;
                    break;
                case R.id.btnPlayBackward:
                    text = mActivity.getResources().getString(R.string.hint_edit_fast_backward);
                    text += " " + -getBackwardSpeed() + "X";
                    break;
                case R.id.btnPlayForward:
                    text = mActivity.getResources().getString(R.string.hint_edit_fast_forward);
                    text += " " + getForwardSpeed() + "X";
                    break;
                case R.id.btnPlay:
                    if (mVdbPlayback.isPaused()) {
                        resId = R.string.hint_edit_resume;
                    } else if (mVdbPlayback.isPlaying()) {
                        resId = R.string.hint_edit_pause;
                    } else {
                        resId = R.string.hint_edit_playback;
                    }
                    break;
                case R.id.btnSelect:
                    if (mVdbImageVideo.getSlideView().isSelecting()) {
                        resId = R.string.hint_edit_cancel_select;
                    } else {
                        resId = R.string.hint_edit_begin_select;
                    }
                    break;
                case R.id.btnDownload:
                    resId = mEditor.isEditingClip() ? R.string.hint_edit_download_selection
                        : R.string.hint_edit_download_playlist;
                    break;
                case R.id.btnMark:
                    resId = R.string.hint_edit_mark_selection;
                    break;
                case R.id.btnEdit:
                    resId = R.string.hint_edit_operations;
                    break;
                case R.id.btnDelete:
                    if (mEditor.isEditingClip()) {
                        resId = R.string.hint_edit_delete_clip;
                    } else if (mEditor.isEditingPlaylist()) {
                        resId = R.string.hint_edit_clear_playlist;
                    }
                    break;
                case R.id.btnShare:
                    resId = R.string.hint_edit_share_video;
                    break;
                default:
                    break;
            }
            if (resId > 0 || text != null) {
                if (resId > 0) {
                    mHintText.setText(resId);
                } else if (text != null) {
                    mHintText.setText(text);
                }
                mHintText.setAlpha(1.0f);
                mHintText.setVisibility(View.VISIBLE);
            }
        } else {
            startButtonHintAnimation(BUTTON_HINT_ANIM_START, BUTTON_HINT_ANIM_DURATION);
        }
    }

    private void startButtonHintAnimation(int startTime, int duration) {
        mHintAnimation = new ViewAnimation() {
            @Override
            protected void onAnimationStart(ViewAnimation animation, boolean bReverse) {
            }

            @Override
            protected void onAnimationDone(ViewAnimation animation, boolean bReverse) {
                mHintText.setVisibility(View.GONE);
            }
        };
        Animation anim = ViewAnimation.createAlphaAnimation(1, 0);
        anim.setStartLengthMs(startTime);
        mHintAnimation.addAnimation(mHintText, anim);
        mHintAnimation.startAnimation(duration, ANIM_STEP, false);
    }

    private void showHintText(int resId) {
        stopHintAnimation();
        mHintText.setText(resId);
        mHintText.setAlpha(1.0f);
        mHintText.setVisibility(View.VISIBLE);
        startButtonHintAnimation(TEXT_HINT_ANIM_START, TEXT_HINT_ANIM_DURATION);
    }

    private String createPictureFileName() {
        long date = mVdbImageVideo.getCurrentDate();
        return date != 0 ? DateTime.toFileName(date) : null;
    }

    private static final String composeFileName(String dir, String fn, int i) {
        if (i == 0)
            return dir + fn + ".jpg";
        else
            return dir + fn + "-" + Integer.toString(i) + ".jpg";
    }

    private void saveToGallery(Bitmap bitmap) {
        String filename = createPictureFileName();
        if (filename != null) {
            try {
                // generate filename that's no conflication
                String dir = Hachi.getPicturePath();
                for (int i = 0; ; i++) {
                    String targetFile = composeFileName(dir, filename, i);
                    File file = new File(targetFile);
                    if (!file.exists()) {
                        filename = targetFile;
                        break;
                    }
                }
                // compress and save to file
                FileOutputStream fos = new FileOutputStream(filename, false);
                try {
                    bitmap.compress(CompressFormat.JPEG, JPEG_QUALITY, fos);
                } finally {
                    fos.close();
                }
                // add to media store
                Hachi.addToMediaStore(mActivity, filename);
                mVdbImageVideo.onImageSaved();
            } catch (Exception ex) {

            }
        }
    }

    private void setToVideoImage() {
        Bitmap bitmap = mVdbPlayback.getCurrentImage();
        if (bitmap != null) {
            mVdbImageVideo.setBitmap(null, bitmap);
        }
    }

    private void onClickImageVideo() {
        if (mActivity.isLandscape()) {
            if (mVdbPlayback.isPlaying()) {
                showSlideControl(!mSlideControlVisible);
            } else if (mVdbImageVideo.getSlideView().isScrolling()) {
                showSlideControl(!mSlideControlVisible);
            } else {
                if (mSlideControlVisible) {
                    if (mVdbImageVideo.isShowSaveImageButtonVisible()) {
                        // S I -> x x
                        showSlideControl(false);
                        mVdbImageVideo.toggleShowSaveImageButton();
                    } else {
                        // S -> S I
                        mVdbImageVideo.toggleShowSaveImageButton();
                    }
                } else {
                    showSlideControl(true);
                    if (mVdbImageVideo.isShowSaveImageButtonVisible()) {
                        mVdbImageVideo.toggleShowSaveImageButton();
                    }
                }
            }
        } else {
            if (!mVdbPlayback.isPlaying() && !mVdbImageVideo.getSlideView().isScrolling()) {
                mVdbImageVideo.toggleShowSaveImageButton();
            }
        }
    }

    class MyVdbPlayback extends VdbPlayback {

        public MyVdbPlayback(BaseActivity activity) {
            super(activity, mVideoControl);
        }

        @Override
        protected boolean requestPlayback() {
            return mVdbImageVideo.requestPlaybackUrl();
        }

        @Override
        protected void preparePlayback() {
            mVdbImageVideo.preparePlayback();
        }

        @Override
        protected void playbackPosChanged(long startTimeMs, int posMs) {
            mbVideoPlaybackPosChanging = true;
            onPlaybackPosChanged(startTimeMs, posMs);
            mbVideoPlaybackPosChanging = false;
        }

        @Override
        protected void playbackStarted(long startTimeMs) {
            mbVideoPlaybackPosChanging = true;
            int pos = onPlaybackPosChanged(startTimeMs, 0);
            mVdbImageVideo.playbackStarted(pos);
            mbVideoPlaybackPosChanging = false;
        }

        @Override
        protected void playbackEnded(long startTimeMs, int lengthMs) {
            mbVideoPlaybackPosChanging = true;
            mbPlaybackEnding = true;
            onPlaybackPosChanged(startTimeMs, lengthMs);
            mVdbImageVideo.playbackEnded();
            if (!mSlideControlVisible && mActivity.isLandscape()) {
                showSlideControl(true);
            }
            mbPlaybackEnding = false;
            mbVideoPlaybackPosChanging = false;
        }

        @Override
        protected boolean shouldFinishPlay() {
            setToVideoImage();
            return !mEditor.mbVideoOnly;
        }

        @Override
        protected void showProgress() {
            mVdbImageVideo.updateProgressPos(true);
        }

        @Override
        protected void hideProgress() {
            mVdbImageVideo.hideProgress();
        }

        @Override
        protected void onViewDown() {
            mVdbImageVideo.onDown();
        }

        @Override
        protected void onViewDoubleClick() {
            mVdbImageVideo.onDoubleClick();
        }

        @Override
        protected void onViewSingleTapUp() {
            mVdbImageVideo.onSingleTapUp();
        }

    }

    class MyVdbEdit extends VdbEdit {

        public MyVdbEdit(BaseActivity activity, SlideView slideView, boolean bShowHint) {
            super(activity, mEditor, slideView, bShowHint);
        }

        @Override
        public boolean requestMarkClip(int index) {
            return mVdbImageVideo.markClipTo(index);
        }

        @Override
        public void requestDeleteClip() {
            // clip/playlist is deleted when animation is done
            CameraVideoEdit.this.doEndEdit(true, false);
        }

        @Override
        public void requestDownload() {
            mVdbImageVideo.requestDownload();
        }

        @Override
        public void onMarkClipOk() {
            showHintText(R.string.msg_mark_clip_ok);
        }

        @Override
        public void onInsertClipOk() {
            showHintText(R.string.msg_insert_clip_ok);
        }

    }

    class MyVdbImageVideo extends VdbImageVideo {

        public MyVdbImageVideo(BaseActivity activity, VdbEditor editor, boolean bShowAcc, VdbPlayback vdbPlayback) {
            super(activity, mVideoControl, mSlideControl, editor, bShowAcc, vdbPlayback);
        }

        @Override
        public void onCursorPosChanged(SlideView slideView, int pos) {
            if (!mbPlaybackEnding) {
                mVdbImageVideo.showAnimation(pos, genAnimationFlags());
            }
            mVdbImageVideo.updatePosInfo(mPosText, mSelectionText);
        }

        @Override
        public void onBeginScroll(SlideView slideView) {
            if (!mEditor.mbVideoOnly) {
                stopPlayback(true);
            }
        }

        @Override
        public void onSelectionChanged(SlideView slideView) {
            mVdbEdit.posterSelectionChanged();
            mVdbImageVideo.updatePosInfo(mPosText, mSelectionText);
        }

        // S: slide view
        // I: save image button

        // S -> S I -> x x -> S

        @Override
        public void onClick() {
            onClickImageVideo();
        }

        @Override
        public void onSaveCurrentImage(Bitmap bitmap) {
            if (bitmap == null) {
                bitmap = mVdbPlayback.getCurrentImage();
            }
            if (bitmap != null) {
                saveToGallery(bitmap);
            }
        }

        @Override
        public void onSeekVideo(int pos_ms) {
            if (!mbVideoPlaybackPosChanging) {
                mVdbPlayback.seekTo(pos_ms);
            }
        }

    }

    class MyVdbMapGoogle extends VdbMapGoogle {

        public MyVdbMapGoogle(BaseActivity activity, RelativeLayout mapControl, VdbEditor editor) {
            super(activity, mapControl, editor);
        }

        @Override
        public void onRequestChangeMap(VdbMapGoogle vdbMap) {
            selectMap(vdbMap);
        }

    }
}
