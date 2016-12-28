package com.waylens.hachi.camera.events;

import android.graphics.Rect;

import com.waylens.hachi.ui.views.RectListView;

import java.util.List;

/**
 * Created by Xiaofei on 2016/12/28.
 */

public class LineListEvent {
    public final List<RectListView.Line> lineList;
    public final Rect sourceRect;

    public LineListEvent(List<RectListView.Line> rectList, Rect sourceRect) {
        this.lineList = rectList;
        this.sourceRect = sourceRect;
    }
}
