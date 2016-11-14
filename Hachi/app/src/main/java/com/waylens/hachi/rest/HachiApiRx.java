package com.waylens.hachi.rest;

import com.waylens.hachi.rest.body.AddMomentViewCountBody;
import com.waylens.hachi.rest.response.SimpleBoolResponse;

import rx.Observable;

/**
 * Created by Xiaofei on 2016/11/14.
 */

public abstract class HachiApiRx {
    public static Observable<SimpleBoolResponse> addMomentViewCount(long momentId) {
        AddMomentViewCountBody body = new AddMomentViewCountBody();
        body.momentID = momentId;
        return HachiService.createHachiApiService().addViewCount(body);
    }
}
