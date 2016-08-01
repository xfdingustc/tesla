package com.waylens.hachi.interactor;





import com.xfdingustc.snipe.vdb.Clip;

import java.util.List;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public interface ClipGridListInteractor {
    void getClipSet();

    void deleteClipList(List<Clip> clipToDelete);
}
