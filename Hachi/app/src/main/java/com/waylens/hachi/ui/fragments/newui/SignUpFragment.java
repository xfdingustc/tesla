package com.waylens.hachi.ui.fragments.newui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.utils.VolleyUtil;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2015/12/9.
 */
public class SignUpFragment extends BaseFragment{
    private static final String TAG = SignUpFragment.class.getSimpleName();

    private RequestQueue mRequestQueue;

    @Bind(R.id.sign_up_email)
    EditText mEtEmail;

    @Bind(R.id.sign_up_password)
    EditText mEtPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_signup_2,
            savedInstanceState);
        initViews();
        return view;
    }



    private void init() {
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());

    }

    private void initViews() {
    }
}
