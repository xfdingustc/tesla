package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;

import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class BaseFragment extends Fragment {

    protected View mRootView;

    MaterialDialog mLoginProgressDialog;

    @NonNull
    protected View createFragmentView(LayoutInflater inflater, ViewGroup container, int layoutResId,
                                      Bundle savedInstanceState) {
        if (mRootView != null) {
            ViewGroup parent = (ViewGroup) mRootView.getParent();
            if (parent != null) {
                parent.removeView(mRootView);
            }
        } else {
            mRootView = inflater.inflate(layoutResId, container, false);
            ButterKnife.bind(this, mRootView);
        }
        return mRootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    public void showToolbar() {
        Toolbar toolbar = ((BaseActivity) getActivity()).getToolbar();
        if (toolbar != null) {
            toolbar.setVisibility(View.VISIBLE);
        }
    }

    public void hideToolbar() {
        Toolbar toolbar = ((BaseActivity) getActivity()).getToolbar();
        if (toolbar != null) {
            toolbar.setVisibility(View.GONE);
        }
    }

    public void setTitle(String title) {
        Toolbar toolbar = ((BaseActivity) getActivity()).getToolbar();
        if (toolbar == null) {
            return;
        }
        toolbar.setTitle(title);
    }

    public void showDialog() {
        if (mLoginProgressDialog != null && mLoginProgressDialog.isShowing()) {
            return;
        }
        mLoginProgressDialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.login)
                .progress(true, 0)
                .progressIndeterminateStyle(false)
                .build();

        mLoginProgressDialog.show();
    }

    public void hideDialog() {
        if (mLoginProgressDialog != null) {
            mLoginProgressDialog.dismiss();
        }
    }

    void showMessage(int resId) {
        Snackbar.make(mRootView, resId, Snackbar.LENGTH_SHORT).show();
    }

    void showMessage(String message) {
        Snackbar.make(mRootView, message, Snackbar.LENGTH_SHORT).show();
    }

}
