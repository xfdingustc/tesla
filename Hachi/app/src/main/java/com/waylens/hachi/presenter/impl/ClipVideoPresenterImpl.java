package com.waylens.hachi.presenter.impl;

import android.content.Context;

import com.waylens.hachi.interactor.ClipVideoInteractor;
import com.waylens.hachi.interactor.impl.ClipVideoInteractorImpl;
import com.waylens.hachi.presenter.Presenter;
import com.waylens.hachi.view.ClipVideoView;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public class ClipVideoPresenterImpl implements Presenter {

    private final Context mContext;
    private final ClipVideoView mClipVideoView;
    private final ClipVideoInteractorImpl mClipVideoInterator;

    public ClipVideoPresenterImpl(Context context, ClipVideoView clipVideoView) {
        if (clipVideoView == null) {
            throw new IllegalArgumentException("Constructor's parameter cannot be null");
        }
        this.mContext = context;
        this.mClipVideoView = clipVideoView;
        this.mClipVideoInterator = new ClipVideoInteractorImpl();
    }

    @Override
    public void initialized() {
        mClipVideoView.initViews(mClipVideoInterator.getPagerFragments(), mClipVideoInterator.getFragmentTitlesRes());
    }
}
