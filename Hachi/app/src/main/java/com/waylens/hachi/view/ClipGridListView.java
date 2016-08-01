package com.waylens.hachi.view;



import com.waylens.hachi.view.base.BaseView;
import com.xfdingustc.snipe.vdb.ClipSet;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public interface ClipGridListView extends BaseView {
    void refreshClipiSet(ClipSet data);
}
