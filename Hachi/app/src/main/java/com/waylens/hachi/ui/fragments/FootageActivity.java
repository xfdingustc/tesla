package com.waylens.hachi.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.eventbus.events.ClipSelectEvent;
import com.waylens.hachi.eventbus.events.ClipSetChangeEvent;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.AddBookmarkRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.toolbox.VdbImageRequest;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.PlaylistEditor;
import com.waylens.hachi.ui.fragments.clipplay2.PlaylistUrlProvider;
import com.waylens.hachi.ui.fragments.clipplay2.UrlProvider;
import com.waylens.hachi.ui.views.cliptrimmer.ClipSetProgressBar;
import com.waylens.hachi.utils.DateTime;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;
import com.waylens.hachi.vdb.ClipSetPos;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;

import butterknife.Bind;
import butterknife.OnClick;


/**
 * Created by Xiaofei on 2016/3/16.
 */
public class FootageActivity extends BaseActivity {
    private static final String TAG = FootageActivity.class.getSimpleName();

    private ClipPlayFragment mClipPlayFragment;

    private ClipSetManager mClipSetManager = ClipSetManager.getManager();

    private static final int DEFAULT_BOOKMARK_LENGTH = 30000;


    private static final String CLIPSET_INDEX = "clipsetindex";

    private int mClipSetIndex;


    private ClipSet mFootageClipSet;
    private ClipSet mBookmarkClipSet;

    private PlaylistEditor mPlaylistEditor;

    private EventBus mEventBus = EventBus.getDefault();

    @Bind(R.id.vsRoot)
    View mRootView;

    @Bind(R.id.clipSetPrgressBar)
    ClipSetProgressBar mClipSetProgressBar;

    @Bind(R.id.btnAddBookmark)
    ImageButton mBtnAddBookmark;

    @Bind(R.id.tvClipPosTime)
    TextView mTvClipPosTime;

    @OnClick(R.id.btnAddBookmark)
    public void onBtnAddBookmarkClicked() {
        doAddBookmark();

    }

    @Subscribe
    public void onEventCameraConnection(CameraConnectionEvent event) {
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_SELECTED_CHANGED:
                initCamera();
                refreshFootageClipSet();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventClipSetPosChanged(ClipSetPosChangeEvent event) {
        ClipSetPos clipSetPos = event.getClipSetPos();
        Clip clip = getClipSet().getClip(clipSetPos.getClipIndex());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss, a");

        mTvClipPosTime.setText(simpleDateFormat.format(DateTime.getTimeDate(clip.getDate(), clipSetPos.getClipTimeMs())));
    }


    public static void launch(Activity activity, int clipSetIndex) {
        Intent intent = new Intent(activity, FootageActivity.class);
        intent.putExtra(CLIPSET_INDEX, clipSetIndex);
        activity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mClipSetIndex = getIntent().getIntExtra(CLIPSET_INDEX, ClipSetManager.CLIP_SET_TYPE_ALLFOOTAGE);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_footage);
        mRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEventBus.post(new ClipSelectEvent(null));
            }
        });
        setupToolbar();
        mClipSetProgressBar.init(mVdbImageLoader, new ClipSetProgressBar.OnBookmarkClickListener() {
            @Override
            public void onBookmarkClick(Clip clip) {

            }
        });
        refreshFootageClipSet();
    }



    @Override
    public void onStart() {
        super.onStart();
        mEventBus.register(this);
        mEventBus.register(mClipSetProgressBar);
    }

    @Override
    public void onStop() {
        super.onStop();
        mEventBus.unregister(this);
        mEventBus.unregister(mClipSetProgressBar);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.cancelAll(new VdbRequestQueue.RequestFilter() {
                @Override
                public boolean apply(VdbRequest<?> request) {
                    if (request instanceof VdbImageRequest) {
//                        Logger.t(TAG).d("cancel image quest");
                        return true;
                    }

                    return false;
                }
            });
        }
    }


    @Override
    public void setupToolbar() {
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        if (mToolbar != null) {
            mToolbar.setNavigationIcon(R.drawable.navbar_close);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
        super.setupToolbar();
    }

    private void setupClipPlayFragment(ClipSet clipSet) {
        mClipSetManager.updateClipSet(mClipSetIndex, clipSet);
        UrlProvider urlProvider1 = new PlaylistUrlProvider(mVdbRequestQueue, 0x101);


        mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, mClipSetIndex, urlProvider1,
            ClipPlayFragment.ClipMode.SINGLE, ClipPlayFragment.CoverMode.BANNER);

        getFragmentManager().beginTransaction().add(R.id.fragmentContainer, mClipPlayFragment).commit();
        Logger.t(TAG).d("clipSet count: " + clipSet.getCount());
        doMakePlaylist();

    }

    private void doMakePlaylist() {
        mPlaylistEditor = new PlaylistEditor(this, mVdtCamera, 0x101);
        mPlaylistEditor.build(mClipSetIndex, new PlaylistEditor.OnBuildCompleteListener() {
            @Override
            public void onBuildComplete(ClipSet clipSet) {
                Logger.t(TAG).d("clipSet count: " + clipSet.getCount());
                mClipSetManager.updateClipSet(mClipSetIndex, clipSet);
                mEventBus.post(new ClipSetChangeEvent(mClipSetIndex));
            }
        });
    }




    private void setupClipProgressBar() {
        mClipSetProgressBar.setClipSet(mFootageClipSet, mBookmarkClipSet);
    }


    private void refreshFootageClipSet() {
        if (mVdbRequestQueue == null) {
            return;
        }

        mFootageClipSet = ClipSetManager.getManager().getClipSet(mClipSetIndex);
        setupClipPlayFragment(mFootageClipSet);
        //setupClipProgressBar(clipSet);
        refreshBookmarkClipSet();

    }

    private void refreshBookmarkClipSet() {
        ClipSetExRequest request = new ClipSetExRequest(Clip.TYPE_MARKED, ClipSetExRequest
            .FLAG_CLIP_EXTRA, new VdbResponse.Listener<ClipSet>() {
            @Override
            public void onResponse(ClipSet response) {
                mBookmarkClipSet = response;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupClipProgressBar();
                    }
                });

            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
    }

    private void doAddBookmark() {
        //Clip clip = mClipSetProgressBar.getSelectClip;

        ClipPos clipPos = mClipSetProgressBar.getCurrentClipPos();
        ClipSetPos clipSetPos = mClipSetProgressBar.getCurrentClipSetPos();
        Clip clip = getClipSet().getClip(clipSetPos.getClipIndex());

        long startTimeMs = clipPos.getClipTimeMs() - DEFAULT_BOOKMARK_LENGTH / 2;
        long endTimeMs = clipPos.getClipTimeMs() + DEFAULT_BOOKMARK_LENGTH / 2;


        startTimeMs = Math.max(startTimeMs, clip.getStartTimeMs());
        endTimeMs = Math.min(endTimeMs, clip.getEndTimeMs());

        AddBookmarkRequest request = new AddBookmarkRequest(clipPos.cid, startTimeMs, endTimeMs, new
            VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    refreshBookmarkClipSet();
                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });

        mVdbRequestQueue.add(request);
    }

    private ClipSet getClipSet() {
        return mClipSetManager.getClipSet(mClipSetIndex);
    }


}
