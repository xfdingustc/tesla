package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.ClipSetChangeEvent;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.AddBookmarkRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.toolbox.VdbImageRequest;
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
import java.util.Date;

import butterknife.Bind;
import butterknife.OnClick;


/**
 * Created by Xiaofei on 2016/3/16.
 */
public class AllFootageFragment extends BaseFragment {
    private static final String TAG = AllFootageFragment.class.getSimpleName();

    private ClipPlayFragment mClipPlayFragment;

    private ClipSetManager mClipSetManager = ClipSetManager.getManager();

    private static final int DEFAULT_BOOKMARK_LENGTH = 30000;


    private final int mClipSetIndex = ClipSetManager.CLIP_SET_TYPE_ALLFOOTAGE;


    private ClipSet mAllFootageClipSet;
    private ClipSet mBookmarkClipSet;

    private PlaylistEditor mPlaylistEditor;

    private EventBus mEventBus = EventBus.getDefault();

    public static AllFootageFragment newInstance() {
        AllFootageFragment fragment = new AllFootageFragment();

        return fragment;
    }

    @Bind(R.id.vsRoot)
    ViewSwitcher mVsRoot;

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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_all_footage, savedInstanceState);
        mClipSetProgressBar.init(mVdbImageLoader, new ClipSetProgressBar.OnBookmarkClickListener() {
            @Override
            public void onBookmarkClick(Clip clip) {
                BottomSheetDialog dialog = new BottomSheetDialog(getActivity());
                dialog.show();
            }
        });

        refreshAllFootageClipSet();

        return view;

    }

    @Override
    public void onStart() {
        super.onStart();
        mEventBus.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mEventBus.unregister(this);
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

    private void onHandleEmptyCamera() {
        if (mVsRoot.getDisplayedChild() == 0) {
            mVsRoot.showNext();
        }
    }

    private void setupClipPlayFragment(ClipSet clipSet) {
        mClipSetManager.updateClipSet(mClipSetIndex, clipSet);
        UrlProvider urlProvider1 = new PlaylistUrlProvider(mVdbRequestQueue, 0x101);


        mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, mClipSetIndex, urlProvider1,
            ClipPlayFragment.ClipMode.SINGLE, ClipPlayFragment.CoverMode.BANNER);

        getChildFragmentManager().beginTransaction().add(R.id.fragmentContainer, mClipPlayFragment).commit();
        doMakePlaylist();

    }

    private void doMakePlaylist() {
        mPlaylistEditor = new PlaylistEditor(getActivity(), mVdtCamera, 0x101);
        mPlaylistEditor.build(mClipSetIndex, new PlaylistEditor.OnBuildCompleteListener() {
            @Override
            public void onBuildComplete(ClipSet clipSet) {
                Logger.t(TAG).d("clipSet count: " + clipSet.getCount());
                mClipSetManager.updateClipSet(mClipSetIndex, clipSet);
                mEventBus.post(new ClipSetChangeEvent(mClipSetIndex));
            }
        });
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventClipSetPosChanged(ClipSetPosChangeEvent event) {
        ClipSetPos clipSetPos = event.getClipSetPos();
        Clip clip = getClipSet().getClip(clipSetPos.getClipIndex());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss, a");

        mTvClipPosTime.setText(simpleDateFormat.format(DateTime.getTimeDate(clip.getDate(), clipSetPos.getClipTimeMs())));
    }

    private void setupClipProgressBar() {
        mClipSetProgressBar.setClipSet(mAllFootageClipSet, mBookmarkClipSet);
    }


    private void refreshAllFootageClipSet() {
        if (mVdbRequestQueue == null) {
            return;
        }
        mVdbRequestQueue.add(new ClipSetExRequest(Clip.TYPE_BUFFERED, ClipSetExRequest.FLAG_CLIP_EXTRA,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet clipSet) {
                    if (clipSet.getCount() == 0) {
                        onHandleEmptyCamera();
                        return;
                    }
                    Logger.t(TAG).d("clipSet number: " + clipSet.getCount());
                    mAllFootageClipSet = clipSet;
                    setupClipPlayFragment(clipSet);
                    //setupClipProgressBar(clipSet);
                    refreshBookmarkClipSet();
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("", error);

                }
            }));
    }

    private void refreshBookmarkClipSet() {
        ClipSetExRequest request = new ClipSetExRequest(Clip.TYPE_MARKED, ClipSetExRequest
            .FLAG_CLIP_EXTRA, new VdbResponse.Listener<ClipSet>() {
            @Override
            public void onResponse(ClipSet response) {
                mBookmarkClipSet = response;
                getActivity().runOnUiThread(new Runnable() {
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
