package com.waylens.hachi.ui.welcome;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.BaseFragment;

import java.io.File;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/11/17.
 */

public class VideoWelcomeFragment extends BaseFragment {
    private static final String TAG = VideoWelcomeFragment.class.getSimpleName();

    @BindView(R.id.video_view)
    VideoView videoView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_video_fragment, savedInstanceState);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
//        String uri = "android.resource://" + getActivity().getPackageName() + "/" + R.raw.smartand;
//        videoView.setVideoURI(Uri.parse(uri));
//        videoView.start();
    }

    @Override
    protected String getRequestTag() {
        return TAG;
    }
}
