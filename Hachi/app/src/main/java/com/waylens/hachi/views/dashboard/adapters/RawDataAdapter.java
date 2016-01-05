package com.waylens.hachi.views.dashboard.adapters;

import android.database.Observable;

import com.waylens.hachi.vdb.RawDataBlock;


/**
 * Created by Xiaofei on 2015/12/18.
 */
public abstract class RawDataAdapter {
    private final AdapterDataObservable mObservable = new AdapterDataObservable();

    public abstract RawDataBlock.RawDataItem getAccDataItem(long pts);

    public abstract RawDataBlock.RawDataItem getObdDataItem(long pts);

    public abstract RawDataBlock.RawDataItem getGpsDataItem(long pts);

    public static abstract class AdapterDataObserver {
        public void onDataChanged(RawDataBlock.RawDataItem dataItem) {

        }


    }

    public void registerAdapterDataObserver(AdapterDataObserver observer) {
        mObservable.registerObserver(observer);
    }

    public void unregisterAdapterDataObserver(AdapterDataObserver observer) {
        mObservable.unregisterObserver(observer);
    }

    public void notifyDataSetChanged(RawDataBlock.RawDataItem dataItem) {
        mObservable.notifyDataChanged(dataItem);
    }

    static class AdapterDataObservable extends Observable<AdapterDataObserver> {
        public boolean hasObservers() {
            return !mObservers.isEmpty();
        }

        public void notifyDataChanged(RawDataBlock.RawDataItem dataItem) {
            for (int i = mObservers.size() - 1; i >=0; i--) {
                mObservers.get(i).onDataChanged(dataItem);
            }
        }
    }
}
