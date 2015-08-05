package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.LoginActivity;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class AccountFragment extends BaseFragment {

    @Bind(R.id.btnAvatar)
    CircleImageView mBtnAvatar;


    @OnClick(R.id.btnAvatar)
    public void onBtnAvatarClicked(){
        LoginActivity.launch(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       return createFragmentView(inflater, container, R.layout.fragment_account, savedInstanceState);
    }
}
