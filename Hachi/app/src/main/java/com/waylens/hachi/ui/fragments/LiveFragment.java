package com.waylens.hachi.ui.fragments;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ViewAnimator;

import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.hardware.VdtCameraManager;
import com.transee.common.GPSPath;
import com.waylens.hachi.ui.activities.DashboardActivity;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.transee.vdb.ImageDecoder;
import com.transee.vdb.Playlist;
import com.transee.vdb.PlaylistSet;
import com.waylens.hachi.vdb.DownloadInfoEx;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RemoteClip;
import com.transee.vdb.RemoteVdb;
import com.transee.vdb.Vdb;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.ui.adapters.ClipSetRecyclerAdapter;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Live Fragment
 * <p/>
 * Created by Xiaofei on 2015/8/4.
 */
public class LiveFragment extends BaseFragment {
    private static final String TAG = "LiveFragment";

    @OnClick(R.id.btnDashboardTest)
    public void onBtnDashboardTestClicked() {
        DashboardActivity.launch(getActivity());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        initViews();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_live, savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void initViews() {

    }


}
