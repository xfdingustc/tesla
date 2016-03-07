package com.waylens.hachi.ui.fragments.clipplay2;

import android.app.DialogFragment;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.activities.ClipModifyActivity;
import com.waylens.hachi.ui.activities.EnhancementActivity;
import com.waylens.hachi.ui.views.dashboard.adapters.LiveRawDataAdapter;
import com.waylens.hachi.ui.views.multisegseekbar.MultiSegSeekbar;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;
import com.waylens.hachi.vdb.RawDataItem;
import com.waylens.hachi.vdb.urls.VdbUrl;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/2/22.
 */
public class ClipPlayFragment extends DialogFragment {
    private static final String TAG = ClipPlayFragment.class.getSimpleName();

    //private ClipSet mClipSet;
    private int mClipSetIndex;

    private VdtCamera mVdtCamera;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;

    private UrlProvider mUrProvider;

    private MediaPlayer mMediaPlayer = new MediaPlayer();

    private Handler mUiHandler;

    private Config mConfig;

    private PositionAdjuster mPositionAdjuster;

    private final int STATE_NONE = 0;
    private final int STATE_PREPAREING = 1;
    private final int STATE_PREPARED = 2;
    private final int STATE_PLAYING = 3;
    private final int STATE_PAUSE = 4;
    private final int STATE_FAST_PREVIEW = 5;

    private int mCurrentState = STATE_NONE;


    @Bind(R.id.textureView)
    TextureView mTextureView;

    @Bind(R.id.clipCover)
    ImageView mClipCover;

    @Bind(R.id.progressLoading)
    ProgressBar mProgressLoading;

    @Bind(R.id.btnPlayPause)
    ImageButton mBtnPlayPause;

    @Bind(R.id.playProgress)
    TextView mTvProgress;

    @Bind(R.id.clipPlayFragToolbar)
    Toolbar mToolbar;

    @Bind(R.id.videoSeekBar)
    SeekBar mSeekBar;


    @Bind(R.id.vsBar)
    ViewSwitcher mVsBar;

    @Bind(R.id.multiSegIndicator)
    MultiSegSeekbar mMultiSegSeekbar;

    @Bind(R.id.wvGauge)
    WebView mWvGauge;


    @OnClick(R.id.btnPlayPause)
    public void onBtnPlayPauseClicked() {
        switch (mCurrentState) {
            case STATE_NONE:
            case STATE_PREPARED:
            case STATE_FAST_PREVIEW:
                start();
                //changeState(STATE_PLAYING);
                refreshProgressBar();
                break;
            case STATE_PAUSE:
                changeState(STATE_PLAYING);
                refreshProgressBar();
                break;
            case STATE_PLAYING:
                changeState(STATE_PAUSE);
                break;
            case STATE_PREPAREING:
                //mPendingAction = PENDING_ACTION_START;
                break;
        }

    }

    private void start() {
        if (mConfig.clipMode == Config.ClipMode.SINGLE) {
            startPreparingClip(getSeekbarTimeMs());
        } else {
            startPreparingClip(0);
        }
    }


    public void setClipSet(ClipSet clipSet) {
        mMultiSegSeekbar.setClipList(getClipSet().getClipList());
        notifyClipSetChanged();
    }

    public void updateGauge(GaugeInfoItem item) {
        String jsApi = "javascript:";
        if (item.isEnable) {
            jsApi += "showGauge";

        } else {
            jsApi += "hideGauge";
        }
        jsApi += "('" + item.title + "')";
        mWvGauge.loadUrl(jsApi);
    }


    public static class Config {
        public enum ClipMode {
            SINGLE,
            MULTI
        }

        public ClipMode clipMode = ClipMode.SINGLE;

    }


    public static ClipPlayFragment newInstance(VdtCamera camera, int clipSetIndex,
                                               UrlProvider vdtUrlProvider,
                                               Config config) {
        ClipPlayFragment fragment = new ClipPlayFragment();
        fragment.mVdtCamera = camera;
        fragment.mClipSetIndex = clipSetIndex;
        fragment.mUrProvider = vdtUrlProvider;
        fragment.mConfig = config;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getShowsDialog()) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        View view = inflater.inflate(R.layout.fragment_clip_play, container, false);
        ButterKnife.bind(this, view);
        initViews();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //mSurfaceView.getHolder().addCallback(this);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

                mBtnPlayPause.setEnabled(true);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getShowsDialog()) {
            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            getDialog().getWindow().setLayout(dm.widthPixels, getDialog().getWindow().getAttributes().height);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaPlayer.stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMediaPlayer.reset();
    }

    private void init() {
        mUiHandler = new Handler();
        mVdbRequestQueue = Snipe.newRequestQueue(getActivity(), mVdtCamera);
        mVdbImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);


    }

    private void initViews() {
        mBtnPlayPause.setEnabled(false);
        setupToolbar();

        Clip clip = getClipSet().getClip(0);
        ClipPos clipPos;
        if (mConfig.clipMode == Config.ClipMode.SINGLE) {
            clipPos = new ClipPos(getClipSet().getClip(0));
        } else {
            clipPos = new ClipPos(clip, 0, ClipPos.TYPE_POSTER, false);
        }
        mVdbImageLoader.displayVdbImage(clipPos, mClipCover);

        if (mConfig.clipMode == Config.ClipMode.MULTI) {
            mVsBar.showNext();
            setupMultiSegSeekBar();
        } else {
            setupSeekBar();
        }

        initGaugeView();

    }

    private void initGaugeView() {
        mWvGauge.getSettings().setJavaScriptEnabled(true);
        mWvGauge.setBackgroundColor(Color.TRANSPARENT);
        mWvGauge.loadUrl("file:///android_asset/api.html");
        //LiveRawDataAdapter mRawDataAdapter = new LiveRawDataAdapter(mVdbRequestQueue, mWvGauge);
    }

    public void showGaugeView(boolean show) {
        if (show) {
            mWvGauge.setVisibility(View.VISIBLE);
        } else {
            mWvGauge.setVisibility(View.GONE);
        }
    }


    private void setupToolbar() {
        mToolbar.setNavigationIcon(R.drawable.navbar_close);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        mToolbar.inflateMenu(R.menu.menu_clip_play_fragment);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.share:
                        return true;
                    case R.id.enhance:
                        dismiss();
                        EnhancementActivity.launch(getActivity(), mClipSetIndex);
                        return true;
                    case R.id.modify:
                        dismiss();
                        ClipModifyActivity.launch(getActivity(), getClipSet().getClip(0));
                        return true;
                }
                return false;
            }
        });

        if (getShowsDialog()) {
            mToolbar.setVisibility(View.VISIBLE);
        } else {
            mToolbar.setVisibility(View.GONE);
        }
    }

    private ClipSet getClipSet() {
        return ClipSetManager.getManager().getClipSet(mClipSetIndex);
    }


    private void setupMultiSegSeekBar() {
        mMultiSegSeekbar.setClipList(getClipSet().getClipList());
        mMultiSegSeekbar.setOnMultiSegSeekbarChangListener(new MultiSegSeekbar.OnMultiSegSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(MultiSegSeekbar seekBar) {
                changeState(STATE_FAST_PREVIEW);
            }

            @Override
            public void onProgressChanged(MultiSegSeekbar seekBar, int progress) {
                if (mCurrentState == STATE_FAST_PREVIEW) {
                    int time = (int) (((float) progress * getClipSet().getTotalSelectedLengthMs()) / seekBar.getMax());
                    ClipPos clipPos = getClipSet().findClipPosByTimePosition(time);
                    if (clipPos != null) {
                        mVdbImageLoader.displayVdbImage(clipPos, mClipCover, true, false);
                    }
                }
            }

            @Override
            public void onStopTrackingTouch(MultiSegSeekbar seekBar) {
                startPreparingClip(getSeekbarTimeMs());
            }
        });
    }


    private void setupSeekBar() {
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                Logger.t(TAG).d("onProgressChanged");
                if (mCurrentState == STATE_FAST_PREVIEW) {
                    long seekBarTimeMs = getSeekbarTimeMs();
                    ClipPos clipPos = new ClipPos(getClipSet().getClip(0), seekBarTimeMs, ClipPos.TYPE_POSTER, false);
                    mVdbImageLoader.displayVdbImage(clipPos, mClipCover, true, false);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Logger.t(TAG).d("onStartTrackingTouch");
                changeState(STATE_FAST_PREVIEW);

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Logger.t(TAG).d("onStopTrackingTouch");
                //startPreparingClip(getSeekbarTimeMs());
            }
        });

        mSeekBar.setMax(getClipSet().getClip(0).getDurationMs());
        mSeekBar.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY));
        mSeekBar.getThumb().setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
    }

    public void setActiveClip(int position, Clip clip) {
        changeState(STATE_FAST_PREVIEW);
        ClipPos clipPos = new ClipPos(clip, 0, ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, mClipCover, true, false);
        mMultiSegSeekbar.setActiveClip(position);
    }

    public void setAudioUrl(String audioUrl) {

    }

    public void showClipPosThumbnail(Clip clip, long timeMs) {
        changeState(STATE_FAST_PREVIEW);
        ClipPos clipPos = new ClipPos(clip, timeMs, ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, mClipCover, true, false);
    }

    public void notifyClipSetChanged() {
        mMultiSegSeekbar.notifyDateSetChanged();
        refreshProgressBar();
    }


    protected void openVideo(VdbUrl url) {
        try {
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Logger.t(TAG).d("Prepare finished!!!");
                    changeState(STATE_PREPARED);

                    changeState(STATE_PLAYING);

                    refreshProgressBar();
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    switch (what) {
                        case MediaPlayer.MEDIA_ERROR_IO:
                            break;
                    }
                    return false;
                }
            });
            mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    switch (what) {
                        case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                            mProgressLoading.setVisibility(View.VISIBLE);
                            break;
                        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                            mProgressLoading.setVisibility(View.GONE);
                            break;
                    }
                    return false;
                }
            });

            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(url.url);
            mMediaPlayer.setSurface(new Surface(mTextureView.getSurfaceTexture()));
            mMediaPlayer.prepareAsync();

        } catch (IOException e) {
            Logger.t(TAG).e("", e);
        }
    }


    private void startPreparingClip(long clipTimeMs) {
        mUrProvider.getUri(clipTimeMs, new UrlProvider.OnUrlLoadedListener() {

            @Override
            public void onUrlLoaded(VdbUrl url) {
                if (url == null) {
                    Snackbar.make(mBtnPlayPause, R.string.get_url_failed, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                Logger.t(TAG).d("Get playback url: " + url.url);
                if (mConfig.clipMode == Config.ClipMode.SINGLE) {
                    mPositionAdjuster = new ClipPositionAdjuster(getClipSet().getClip(0), url);
                } else {
                    mPositionAdjuster = new PlaylistPositionAdjuster(url);
                }
                openVideo(url);
            }


        });

        changeState(STATE_PREPAREING);
    }

    private void changeState(int targetState) {
        switch (targetState) {
            case STATE_PREPAREING:
                mClipCover.setVisibility(View.VISIBLE);
                mProgressLoading.setVisibility(View.VISIBLE);
                break;
            case STATE_PREPARED:
                mProgressLoading.setVisibility(View.GONE);
                break;
            case STATE_PLAYING:
                mClipCover.setVisibility(View.INVISIBLE);
                mBtnPlayPause.setImageResource(R.drawable.playbar_pause);
                mMediaPlayer.start();
                mProgressLoading.setVisibility(View.GONE);
                break;
            case STATE_PAUSE:
                mBtnPlayPause.setImageResource(R.drawable.playbar_play);
                mMediaPlayer.pause();
                break;
            case STATE_FAST_PREVIEW:
                mClipCover.setVisibility(View.VISIBLE);
                mBtnPlayPause.setImageResource(R.drawable.playbar_play);
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                break;
        }
        mCurrentState = targetState;
    }

    private void refreshProgressBar() {
        int currentPos = mMediaPlayer.getCurrentPosition();
        if (mPositionAdjuster != null) {
            currentPos = mPositionAdjuster.getAdjustedPostion(currentPos);
        }

        int duration = getClipSet().getTotalSelectedLengthMs();

//        Logger.t(TAG).d("duration: " + duration + " currentPos: " + currentPos);

        String timeText = DateUtils.formatElapsedTime(currentPos / 1000) + "/" + DateUtils
                .formatElapsedTime(duration / 1000);

        mTvProgress.setText(timeText);

        if (mConfig.clipMode == Config.ClipMode.SINGLE) {
            mSeekBar.setProgress(currentPos);
        } else {
            int progress = (int) ((float) currentPos * mMultiSegSeekbar.getMax() / duration);
            mMultiSegSeekbar.setProgress(progress);
        }

        if (mMediaPlayer.isPlaying()) {
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshProgressBar();
                }
            }, 50);
        }
    }


    public long getSeekbarTimeMs() {
        if (mConfig.clipMode == Config.ClipMode.SINGLE) {
            Clip clip = getClipSet().getClip(0);
            return clip.getStartTimeMs() + ((long) clip.getDurationMs() * mSeekBar.getProgress()) / mSeekBar.getMax();
        } else {
            return ((long) getClipSet().getTotalSelectedLengthMs() * mMultiSegSeekbar.getProgress()) / mMultiSegSeekbar.getMax();
        }
    }


}
