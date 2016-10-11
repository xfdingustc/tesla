package com.waylens.hachi.interactor.impl;

import android.widget.Toast;

import com.orhanobut.logger.Logger;

import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.interactor.ClipGridListInteractor;
import com.waylens.hachi.listeners.BaseSingleLoadedListener;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.reative.SnipeApiRx;
import com.waylens.hachi.snipe.toolbox.ClipDeleteRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipSet;


import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public class ClipGridListInteractorImpl implements ClipGridListInteractor {
    private static final String TAG = ClipGridListInteractorImpl.class.getSimpleName();
    private final String mRequestTag;
    private final int mClipSetType;
    private final int mFlag;
    private final int mAttr;


    private BaseSingleLoadedListener<ClipSet> mLoadListener;

    public ClipGridListInteractorImpl(String requestTag, int clipSetType, int flag, int attr,
                                      BaseSingleLoadedListener<ClipSet> loadedListener) {
        this.mRequestTag = requestTag;
        this.mClipSetType = clipSetType;
        this.mFlag = flag;
        this.mAttr = attr;
        this.mLoadListener = loadedListener;
    }

    @Override
    public void getClipSet() {
        SnipeApiRx.getClipSetRx(mClipSetType, mFlag, mAttr)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<ClipSet>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    mLoadListener.onError(e.getMessage());
                }

                @Override
                public void onNext(ClipSet clipSet) {
                    mLoadListener.onSuccess(clipSet);
                }
            });

    }

    @Override
    public void deleteClipList(final List<Clip> clipToDelete) {

        Observable.from(clipToDelete)
            .concatMap(new Func1<Clip, Observable<Integer>>() {
                @Override
                public Observable<Integer> call(Clip clip) {
                    return SnipeApiRx.deleteClipRx(clip.cid);
                }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Integer>(){

                @Override
                public void onCompleted() {
                    getClipSet();
                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Integer integer) {

                }
            });
    }
}
