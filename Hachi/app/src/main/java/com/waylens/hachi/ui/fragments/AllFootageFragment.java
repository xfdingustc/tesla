package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ViewSwitcher;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.AddBookmarkRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.ClipUrlProvider;
import com.waylens.hachi.ui.fragments.clipplay2.UrlProvider;
import com.waylens.hachi.ui.views.cliptrimmer.ClipSetProgressBar;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/3/16.
 */
public class AllFootageFragment extends BaseFragment {
    private static final String TAG = AllFootageFragment.class.getSimpleName();

    private ClipPlayFragment mClipPlayFragment;

    private ClipSetManager mClipSetManager = ClipSetManager.getManager();


    private final int mClipSetIndex = ClipSetManager.CLIP_SET_TYPE_ALLFOOTAGE;


    private ClipSet mAllFootageClipSet;
    private ClipSet mBookmarkClipSet;

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
        mClipSetProgressBar.init(mVdbImageLoader);

        refreshAllFootageClipSet();

        return view;

    }

    @Override
    public void onFragmentFocused(boolean focused) {
        if (focused) {
            refreshBookmarkClipSet();
        }
    }


    private void onHandleEmptyCamera() {
        if (mVsRoot.getDisplayedChild() == 0) {
            mVsRoot.showNext();
        }
    }

    private void setupClipPlayFragment(ClipSet clipSet) {
        mClipSetManager.updateClipSet(mClipSetIndex, clipSet);
        UrlProvider urlProvider = new ClipUrlProvider(mVdbRequestQueue, getClipSet().getClip(0).cid,
            getClipSet().getClip(0).getDurationMs());
        ClipPlayFragment.Config config = new ClipPlayFragment.Config();
        config.clipMode = ClipPlayFragment.Config.ClipMode.SINGLE;
        mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, mClipSetIndex, urlProvider,
            config);

        getChildFragmentManager().beginTransaction().add(R.id.fragmentContainer, mClipPlayFragment).commit();


    }

    private void setupClipProgressBar() {
        mClipSetProgressBar.setClipSet(mAllFootageClipSet, mBookmarkClipSet);
        mClipSetProgressBar.setOnSeekBarChangeListener(new ClipSetProgressBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(ClipSetProgressBar progressBar) {

            }

            @Override
            public void onProgressChanged(ClipSetProgressBar progressBar, ClipPos clipPos, boolean fromUser) {
                if (clipPos != null) {
                    mClipPlayFragment.showThumbnail(clipPos);
                }
            }

            @Override
            public void onStopTrackingTouch(ClipSetProgressBar progressBar) {

            }
        });

    }


    private void refreshAllFootageClipSet() {
        if (mVdbRequestQueue == null) {
            return;
        }
        mVdbRequestQueue.add(new ClipSetRequest(Clip.TYPE_BUFFERED, ClipSetRequest.FLAG_CLIP_EXTRA,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet clipSet) {
                    if (clipSet.getCount() == 0) {
                        onHandleEmptyCamera();
                        return;
                    }
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
        ClipSetRequest request = new ClipSetRequest(Clip.TYPE_MARKED, ClipSetRequest
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
        long startTimeMs = clipPos.getClipTimeMs() - 15000;
        long endTimeMs = clipPos.getClipTimeMs() + 15000;

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
