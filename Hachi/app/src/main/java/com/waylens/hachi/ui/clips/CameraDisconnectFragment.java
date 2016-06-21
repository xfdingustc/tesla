package com.waylens.hachi.ui.clips;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.BaseFragment;

/**
 * Created by Xiaofei on 2016/6/21.
 */
public class CameraDisconnectFragment extends BaseFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_camera_disconnected, savedInstanceState);
    }
}
