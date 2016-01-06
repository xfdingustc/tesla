package com.waylens.hachi.views.dashboard.adapters;

import android.database.Observable;

import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;


/**
 * Created by Xiaofei on 2015/12/18.
 */
public abstract class RawDataAdapter {
    private final AdapterDataObservable mObservable = new AdapterDataObservable();


    public static abstract class AdapterDataObserver {
        public void onDataChanged(RawDataItem dataItem) {

        }


    }

    public void registerAdapterDataObserver(AdapterDataObserver observer) {
        mObservable.registerObserver(observer);
    }

    public void unregisterAdapterDataObserver(AdapterDataObserver observer) {
        mObservable.unregisterObserver(observer);
    }

    public void notifyDataSetChanged(RawDataItem dataItem) {
        mObservable.notifyDataChanged(dataItem);
    }

    static class AdapterDataObservable extends Observable<AdapterDataObserver> {
        public boolean hasObservers() {
            return !mObservers.isEmpty();
        }

        public void notifyDataChanged(RawDataItem dataItem) {
            for (int i = mObservers.size() - 1; i >=0; i--) {
                mObservers.get(i).onDataChanged(dataItem);
            }
        }
    }
}
