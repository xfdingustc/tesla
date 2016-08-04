package com.waylens.hachi.ui.clips.playlist;

import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.eventbus.events.ClipSetChangeEvent;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.xfdingustc.snipe.SnipeError;
import com.xfdingustc.snipe.VdbRequest;
import com.xfdingustc.snipe.VdbRequestFuture;
import com.xfdingustc.snipe.VdbRequestQueue;
import com.xfdingustc.snipe.VdbResponse;
import com.xfdingustc.snipe.toolbox.ClipSetExRequest;
import com.xfdingustc.snipe.toolbox.PlaylistEditRequest;
import com.xfdingustc.snipe.vdb.Clip;
import com.xfdingustc.snipe.vdb.ClipSet;
import com.xfdingustc.snipe.vdb.ClipSetManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * Created by Xiaofei on 2016/6/16.
 */
public class PlayListEditor {
    private static final String TAG = PlayListEditor.class.getSimpleName();

    private VdbRequestQueue mVdbRequestQueue;
    private final int mPlayListId;


    private ClipSet mClipSet;

    private int mClipAdded;

    private EventBus mEventBus = EventBus.getDefault();


    @Subscribe
    public void onEventClipSetChanged(ClipSetChangeEvent event) {
        Logger.t(TAG).d("receive event " + event.getNeedRebuildList());
        if (event.getNeedRebuildList()) {
            rebuildPlayListRx()
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Void aVoid) {

                    }
                });
        }
    }

    public PlayListEditor(@NonNull VdbRequestQueue requestQueue, int playListId) {
        this.mVdbRequestQueue = requestQueue;
        this.mPlayListId = playListId;
        this.mClipSet = new ClipSet(playListId);
    }

    public int getPlaylistId() {
        return mPlayListId;
    }


    public void reconstruct() {
        mClipSet = ClipSetManager.getManager().getClipSet(mPlayListId);
    }

    public Observable<Void> buildRx(Clip clip) {
        List<Clip> clipList = new ArrayList<>();
        clipList.add(clip);
        return buildRx(clipList);
    }


    public Observable<Void> buildRx(List<Clip> clipList) {
        mClipSet.clear();
        for (Clip clip : clipList) {
            mClipSet.addClip(clip);
        }
        ClipSetManager.getManager().updateClipSet(mPlayListId, mClipSet);

        return rebuildPlayListRx();
    }

    public Observable<Void> addRx(List<Clip> clipList) {
        for (Clip clip : clipList) {
            mClipSet.addClip(clip);
        }
        return rebuildPlayListRx();
    }

    public Observable<Void> removeRx(int position) {
        mClipSet.remove(position);
        return rebuildPlayListRx();
    }

    private Observable<Void> rebuildPlayListRx() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    doClearPlayListSync();
                    doRebuildPlayListSync();
                    doGetPlaylistInfoSync();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    public void cancel() {
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.cancelAll(TAG);
        }
    }




    private void doClearPlayListSync() throws ExecutionException, InterruptedException {
        VdbRequestFuture<Integer> vdbRequestFuture = VdbRequestFuture.newFuture();
        PlaylistEditRequest request = PlaylistEditRequest.getClearPlayListRequest(mPlayListId,
            vdbRequestFuture, vdbRequestFuture);
        add2RequestQueue(request);
        Integer result = vdbRequestFuture.get();
    }

    private void doRebuildPlayListSync() throws ExecutionException, InterruptedException {
        for (final Clip clip : mClipSet.getClipList()) {
            VdbRequestFuture<Integer> vdbRequestFuture = VdbRequestFuture.newFuture();
            PlaylistEditRequest playRequest = new PlaylistEditRequest(clip, clip.editInfo.selectedStartValue,
                clip.editInfo.selectedEndValue, mPlayListId, vdbRequestFuture, vdbRequestFuture);
            add2RequestQueue(playRequest);
            Integer result = vdbRequestFuture.get();
        }
    }


    private void doGetPlaylistInfoSync() throws ExecutionException, InterruptedException {
        VdbRequestFuture<ClipSet> vdbRequestFuture = VdbRequestFuture.newFuture();
        ClipSetExRequest request = new ClipSetExRequest(mPlayListId, ClipSetExRequest.FLAG_CLIP_EXTRA,
            vdbRequestFuture, vdbRequestFuture);

        add2RequestQueue(request);

        ClipSet clipSet = vdbRequestFuture.get();
        adjustClipSet(clipSet);
    }

    private void adjustClipSet(ClipSet clipSet) {
        Logger.t(TAG).d("origin count: " + mClipSet.getCount() + " new clip: " + clipSet.getCount());
        for (int i = 0; i < mClipSet.getCount(); i++) {
            Clip originClip = mClipSet.getClip(i);
            Clip newClip = clipSet.getClip(i);
            if (newClip != null) {
                originClip.editInfo.selectedStartValue = newClip.getStartTimeMs();
                originClip.editInfo.selectedEndValue = newClip.getEndTimeMs();
                if (originClip.editInfo.selectedStartValue < originClip.editInfo.minExtensibleValue) {
                    originClip.editInfo.minExtensibleValue = originClip.editInfo.selectedStartValue;
                }

                if (originClip.editInfo.selectedEndValue > originClip.editInfo.maxExtensibleValue) {
                    originClip.editInfo.maxExtensibleValue = originClip.editInfo.selectedEndValue;
                }
            }
        }


        mEventBus.post(new ClipSetChangeEvent(mPlayListId, false));
    }

    private void add2RequestQueue(VdbRequest request) throws IllegalStateException {
        mVdbRequestQueue = VdtCameraManager.getManager().getCurrentVdbRequestQueue();
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.add(request.setTag(TAG));
        } else {
            throw new IllegalStateException("Camera disconnect");
        }
    }


}
