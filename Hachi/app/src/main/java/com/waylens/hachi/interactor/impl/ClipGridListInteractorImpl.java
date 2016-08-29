package com.waylens.hachi.interactor.impl;

import android.widget.Toast;

import com.orhanobut.logger.Logger;

import com.waylens.hachi.interactor.ClipGridListInteractor;
import com.waylens.hachi.listeners.BaseSingleLoadedListener;
import com.xfdingustc.snipe.SnipeError;
import com.xfdingustc.snipe.VdbRequestFuture;
import com.xfdingustc.snipe.VdbRequestQueue;
import com.xfdingustc.snipe.VdbResponse;
import com.xfdingustc.snipe.control.VdtCamera;
import com.xfdingustc.snipe.control.VdtCameraManager;
import com.xfdingustc.snipe.toolbox.ClipDeleteRequest;
import com.xfdingustc.snipe.toolbox.ClipSetExRequest;
import com.xfdingustc.snipe.vdb.Clip;
import com.xfdingustc.snipe.vdb.ClipSet;


import java.util.ArrayList;
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


        this.mRequestTag = requestTag;
        this.mClipSetType = clipSetType;
        this.mFlag = flag;
        this.mAttr = attr;
        this.mLoadListener = loadedListener;
    }

    @Override
    public void getClipSet() {

        ClipSetExRequest request = new ClipSetExRequest(mClipSetType, mFlag | ClipSetExRequest.FLAG_CLIP_DESC, mAttr, new VdbResponse.Listener<ClipSet>() {
            @Override
            public void onResponse(ClipSet response) {
                ArrayList<Clip> clipList = response.getClipList();
                String vin = null;
                for( Clip clip : clipList) {
                    Logger.t(TAG).d("Vin  = " + clip.getVin());
                    if (clip.getVin() != null) {
                        vin = clip.getVin();
                    }
                }
                Logger.t(TAG).d("Get Inserted response\tvin = " + vin);
                mLoadListener.onSuccess(response);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        request.setTag(mRequestTag);

        mVdbRequestQueue = VdtCameraManager.getManager().getCurrentVdbRequestQueue();


        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.add(request);
        } else {
            mLoadListener.onError("no camera connected");
        }
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
