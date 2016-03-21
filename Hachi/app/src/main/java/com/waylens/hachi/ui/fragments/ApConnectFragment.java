package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.waylens.hachi.R;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/3/21.
 */
public class ApConnectFragment extends BaseFragment {

    private String mSSID;
    private String mPassword;

    public static ApConnectFragment newInstance(String ssid, String password) {
        ApConnectFragment fragment = new ApConnectFragment();
        Bundle bundle = new Bundle();
        bundle.putString("ssid", ssid);
        bundle.putString("password", password);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Bind(R.id.tvSsid)
    TextView mTvSsid;

    @Bind(R.id.tvPassword)
    TextView mTvPassword;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_ap_connect, savedInstanceState);
        initViews();
        return view;
    }




    private void init() {
        Bundle bundle = getArguments();
        mSSID = bundle.getString("ssid");
        mPassword = bundle.getString("password");
    }

    private void initViews() {
        mTvSsid.setText("SSID:" + mSSID);
        mTvPassword.setText("PASSWORD:" + mPassword);
    }

}
