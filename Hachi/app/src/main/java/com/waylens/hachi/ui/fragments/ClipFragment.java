package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/4/20.
 */
public class ClipFragment extends BaseFragment {
    private static final String TAG = ClipFragment.class.getSimpleName();

    public static ClipFragment newInstance() {
        ClipFragment fragment = new ClipFragment();
//        Bundle args = new Bundle();
//        args.putInt(ARG_CLIP_SET_TYPE, clipSetType);
//        args.putBoolean(ARG_IS_MULTIPLE_MODE, isMultipleSelectionMode);
//        args.putBoolean(ARG_IS_ADD_MORE, isAddMore);
//        fragment.setArguments(args);
        return fragment;
    }

    @Bind(R.id.refreshLayout)
    SwipeRefreshLayout mRefreshLayout;


    @Bind(R.id.menualClipGroupList)
    RecyclerView mMenualClipGroupList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        return super.onCreateView(inflater, container, savedInstanceState);
        View view = createFragmentView(inflater, container, R.layout.fragment_clip, savedInstanceState);
        doGetAllMenualClipSet();
        return view;
    }


    private void doGetAllMenualClipSet() {
        if (mVdbRequestQueue == null) {
            return;
        }
        mVdbRequestQueue.add(new ClipSetExRequest(Clip.TYPE_BUFFERED, ClipSetExRequest.FLAG_CLIP_EXTRA | ClipSetExRequest.FLAG_CLIP_ATTR,
            Clip.CLIP_ATTR_MANUALLY, new VdbResponse.Listener<ClipSet>() {
            @Override
            public void onResponse(ClipSet clipSet) {
                if (clipSet.getCount() == 0) {
//                        onHandleEmptyCamera();
                    return;
                }
                Logger.t(TAG).d("clipSet number: " + clipSet.getCount());
//                    mAllFootageClipSet = clipSet;
//                    setupClipPlayFragment(clipSet);
//                    //setupClipProgressBar(clipSet);
//                    refreshBookmarkClipSet();
            }
        },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("", error);

                }
            }));
    }
}
