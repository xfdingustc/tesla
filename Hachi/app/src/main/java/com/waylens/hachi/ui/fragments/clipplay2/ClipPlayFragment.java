package com.waylens.hachi.ui.fragments.clipplay2;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/2/22.
 */
public class ClipPlayFragment extends Fragment {

    @Bind(R.id.clipCover)
    ImageView mClipCover;

    private Clip mClip;
    private VdtCamera mVdtCamera;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;

    public static ClipPlayFragment newInstance(VdtCamera camera, Clip clip) {
        ClipPlayFragment fragment = new ClipPlayFragment();
        fragment.mVdtCamera = camera;
        fragment.mClip = clip;
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
        View view = inflater.inflate(R.layout.fragment_clip_play, container, false);
        ButterKnife.bind(this, view);
        initViews();
        return view;
    }

    private void init() {
        mVdbRequestQueue = Snipe.newRequestQueue(getActivity(), mVdtCamera);
        mVdbImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
    }

    private void initViews() {
        ClipPos clipPos = new ClipPos(mClip);
        mVdbImageLoader.displayVdbImage(clipPos, mClipCover);
    }
}
