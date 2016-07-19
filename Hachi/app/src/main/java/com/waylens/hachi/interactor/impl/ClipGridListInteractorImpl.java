package com.waylens.hachi.interactor.impl;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.interactor.ClipGridListInteractor;
import com.waylens.hachi.library.vdb.Clip;
import com.waylens.hachi.library.vdb.ClipSet;
import com.waylens.hachi.listeners.BaseSingleLoadedListener;
import com.waylens.hachi.library.snipe.SnipeError;
import com.waylens.hachi.library.snipe.VdbRequestFuture;
import com.waylens.hachi.library.snipe.VdbRequestQueue;
import com.waylens.hachi.library.snipe.VdbResponse;
import com.waylens.hachi.library.snipe.toolbox.ClipDeleteRequest;
import com.waylens.hachi.library.snipe.toolbox.ClipSetExRequest;


import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
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

    private VdtCamera mVdtCamera;
    private VdbRequestQueue mVdbRequestQueue;
    private BaseSingleLoadedListener<ClipSet> mLoadListener;

    public ClipGridListInteractorImpl(String requestTag, int clipSetType, int flag, int attr,
                                      BaseSingleLoadedListener<ClipSet> loadedListener) {
        this.mVdtCamera = VdtCameraManager.getManager().getCurrentCamera();
        if (mVdtCamera != null) {
            mVdbRequestQueue = mVdtCamera.getRequestQueue();
        }

        this.mRequestTag = requestTag;
        this.mClipSetType = clipSetType;
        this.mFlag = flag;
        this.mAttr = attr;
        this.mLoadListener = loadedListener;
    }

    @Override
    public void getClipSet() {
        if (mVdbRequestQueue == null) {
            mLoadListener.onError("camera is disconnected");
        }

        Logger.t(TAG).d("getClipSet");


        ClipSetExRequest request = new ClipSetExRequest(mClipSetType, mFlag, mAttr, new VdbResponse.Listener<ClipSet>() {
            @Override
            public void onResponse(ClipSet response) {
                mLoadListener.onSuccess(response);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        request.setTag(mRequestTag);
        mVdbRequestQueue.add(request);
    }

    @Override
    public void deleteClipList(final List<Clip> clipToDelete) {
        Observable.create(new Observable.OnSubscribe<ClipSet>() {
            @Override
            public void call(Subscriber<? super ClipSet> subscriber) {
                for (Clip clip : clipToDelete) {
                    VdbRequestFuture<Integer> requestFuture = VdbRequestFuture.newFuture();
                    ClipDeleteRequest request = new ClipDeleteRequest(clip.cid, requestFuture, requestFuture);
                    request.setTag(mRequestTag);
                    mVdbRequestQueue.add(request);

                    try {
                        int response = requestFuture.get();
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }
                }

                VdbRequestFuture<ClipSet> clipSetRequestFuture = VdbRequestFuture.newFuture();
                ClipSetExRequest request = new ClipSetExRequest(mClipSetType, mFlag, mAttr, clipSetRequestFuture, clipSetRequestFuture);
                request.setTag(mRequestTag);
                mVdbRequestQueue.add(request);

                try {
                    ClipSet newClipSet = clipSetRequestFuture.get();
                    subscriber.onNext(newClipSet);
                } catch (Exception e) {
                    subscriber.onError(e);
                }


            }
        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<ClipSet>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    mLoadListener.onException(e.getMessage());
                }

                @Override
                public void onNext(ClipSet clipSet) {
                    mLoadListener.onSuccess(clipSet);
                }
            });
    }
}
