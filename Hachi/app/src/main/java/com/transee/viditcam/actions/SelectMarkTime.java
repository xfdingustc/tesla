package com.transee.viditcam.actions;

import android.app.Activity;

import com.waylens.hachi.R;

import java.util.Arrays;

/**
 * Created by Richard on 9/17/15.
 */
public abstract class SelectMarkTime extends SingleSelect {

    private int mMarkTimeIndex;
    private String[] mMarkTimesDesc;

    public SelectMarkTime(Activity activity, String[] markTimesDesc, int markTimeIndex) {
        super(activity);
        mMarkTimesDesc = markTimesDesc;
        mMarkTimeIndex = markTimeIndex;
    }

    public abstract void onSelectMarkTime(int id);

    @Override
    protected void onSelectItem(int id) {
        onSelectMarkTime(id);
    }

    @Override
    public void show() {
        for (int i = 0; i < mMarkTimesDesc.length - 1; i++) {
                addItem(mMarkTimesDesc[i], i);
        }
        setSelId(mMarkTimeIndex);
        super.show();
    }
}
