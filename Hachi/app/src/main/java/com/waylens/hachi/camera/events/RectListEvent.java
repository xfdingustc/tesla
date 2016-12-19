package com.waylens.hachi.camera.events;

import android.graphics.Rect;

import java.util.List;

/**
 * Created by Xiaofei on 2016/12/19.
 */

public class RectListEvent {
    public final List<Rect> rectList;
    public final Rect sourceRect;

    public RectListEvent(List<Rect> rectList, Rect sourceRect) {
        this.rectList = rectList;
        this.sourceRect = sourceRect;
    }
}
