package com.waylens.hachi.presenter.impl;

import android.content.Context;

import com.waylens.hachi.interactor.ClipGridListInteractor;
import com.waylens.hachi.interactor.impl.ClipGridListInteractorImpl;
import com.waylens.hachi.listeners.BaseSingleLoadedListener;
import com.waylens.hachi.presenter.ClipGridListPresenter;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.view.ClipGridListView;


import java.util.List;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public class ClipGridListPresenterImpl implements ClipGridListPresenter, BaseSingleLoadedListener<ClipSet> {
    private static final String TAG = ClipGridListInteractorImpl.class.getSimpleName();

    private final Context mContext;
    private final ClipGridListView mGridListView;
    private final ClipGridListInteractor mClipGridListInteractor;

    public ClipGridListPresenterImpl(Context context, int clipSetType, int flag,
                                     int attr, ClipGridListView gridListView) {
        this.mContext = context;
        this.mGridListView = gridListView;
        this.mClipGridListInteractor = new ClipGridListInteractorImpl(clipSetType, flag, attr, this);
    }


    @Override
    public void loadClipSet(boolean isSwipeRefresh) {
        mGridListView.hideLoading();
        if (!isSwipeRefresh) {
            mGridListView.showLoading("loading...");
        }

        mClipGridListInteractor.getClipSet();
    }

    @Override
    public void deleteClipList(List<Clip> clipsToDelete) {
        mClipGridListInteractor.deleteClipList(clipsToDelete);
    }

    @Override
    public void onItemClickListener() {

    }

    @Override
    public void onSuccess(ClipSet data) {
        mGridListView.hideLoading();
        mGridListView.refreshClipiSet(data);
        if (data.getCount() == 0) {
            mGridListView.showEmpty();
        }
    }

    @Override
    public void onError(String msg) {
        mGridListView.showCameraDisconnect();
    }

    @Override
    public void onException(String msg) {

    }
}
