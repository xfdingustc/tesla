package com.waylens.hachi.snipe.reative;

import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipSet;

import java.util.concurrent.ExecutionException;

import rx.Observable;
import rx.functions.Func0;


/**
 * Created by Xiaofei on 2016/10/11.
 */

public class SnipeApiRx {
    public static Observable<ClipSet> getClipSetRx(final int type, final int flag, final int attr) {
        return Observable.defer(new Func0<Observable<ClipSet>>() {
            @Override
            public Observable<ClipSet> call() {
                try {
                    return Observable.just(SnipeApi.getClipSet(type, flag, attr));
                } catch (ExecutionException | InterruptedException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public static Observable<Integer> deleteClipRx(final Clip.ID cid) {
        return Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                try {
                    return Observable.just(SnipeApi.deleteClip(cid));
                } catch (ExecutionException  | InterruptedException e) {
                    return Observable.error(e);
                }
            }
        });
    }
}
