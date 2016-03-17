package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

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

    private boolean mIsAddingBookmark = false;

    public static AllFootageFragment newInstance() {
        AllFootageFragment fragment = new AllFootageFragment();

        return fragment;
    }

    @Bind(R.id.clipSetPrgressBar)
    ClipSetProgressBar mClipSetProgressBar;

    @Bind(R.id.btnAddBookmark)
    ImageButton mBtnAddBookmark;

    @OnClick(R.id.btnAddBookmark)
    public void onBtnAddBookmarkClicked() {
        if (mIsAddingBookmark == false) {
            mBtnAddBookmark.setImageDrawable(getResources().getDrawable(R.drawable.btn_enhance_addbookmark_s));
        } else {
            mBtnAddBookmark.setImageDrawable(getResources().getDrawable(R.drawable.btn_enhance_addbookmark_n));
            doAddBookmark();
        }
        mIsAddingBookmark = !mIsAddingBookmark;
        mClipSetProgressBar.toggleSelectMode(mIsAddingBookmark);
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_all_footage,
            savedInstanceState);
        getAllClips();

        return view;

    }

    private void getAllClips() {
        mVdbRequestQueue.add(new ClipSetRequest(Clip.TYPE_BUFFERED, ClipSetRequest.FLAG_CLIP_EXTRA,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet clipSet) {
                    //calculateClipSetGroup(clipSet);
                    //setupClipSetGroupView();
                    if (clipSet.getCount() == 0) {
                        onHandleEmptyCamera();
                    }
                    setupClipPlayFragment(clipSet);
                    setupClipProgressBar(clipSet);

                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("", error);

                }
            }));
    }


    private void onHandleEmptyCamera() {

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

    private void setupClipProgressBar(final ClipSet clipSet) {
        mClipSetProgressBar.setClipSet(clipSet, mVdbImageLoader);
        mClipSetProgressBar.setOnSeekBarChangeListener(new ClipSetProgressBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(ClipSetProgressBar progressBar) {

            }

            @Override
            public void onProgressChanged(ClipSetProgressBar progressBar, long progress, boolean fromUser) {

                ClipSet clipSet = getClipSet();
                ClipPos clipPos = clipSet.findClipPosByTimePosition((int) progress);
//                Logger.t(TAG).d("Progress is : " + progress + " clipPos: " + clipPos);
                mClipPlayFragment.showThumbnail(clipPos);
            }

            @Override
            public void onStopTrackingTouch(ClipSetProgressBar progressBar) {

            }
        });


        ClipSetRequest request = new ClipSetRequest(Clip.TYPE_MARKED, ClipSetRequest
            .FLAG_CLIP_EXTRA, new VdbResponse.Listener<ClipSet>() {
            @Override
            public void onResponse(ClipSet response) {
                mClipSetProgressBar.setBookmarkClipSet(response);
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
        long startTimeMs = mClipSetProgressBar.getSelectStartTimeMs();
        long endTimeMs = mClipSetProgressBar.getSelectEndTimeMs();

        ClipPos clipPos = getClipSet().findClipPosByTimePosition((int)startTimeMs);

        AddBookmarkRequest request = new AddBookmarkRequest(clipPos.cid, startTimeMs, endTimeMs, new
            VdbResponse.Listener<Integer>() {
            @Override
            public void onResponse(Integer response) {

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
