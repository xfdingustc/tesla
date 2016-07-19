package com.waylens.hachi.presenter;



import com.waylens.hachi.library.vdb.Clip;

import java.util.List;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public interface ClipGridListPresenter {
    void loadClipSet(boolean isSwipeRefresh);

    void deleteClipList(List<Clip> clipsToDelete);

    void onItemClickListener();
}
