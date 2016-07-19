package com.waylens.hachi.view;


import com.waylens.hachi.library.vdb.ClipSet;
import com.waylens.hachi.view.base.BaseView;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public interface ClipGridListView extends BaseView {
    void refreshClipiSet(ClipSet data);
}
