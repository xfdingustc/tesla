package com.waylens.hachi.vdb;

import com.waylens.hachi.snipe.BasicVdbSocket;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbSocket;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Xiaofei on 2016/3/4.
 */
public class ClipSetManager {
    private Map<Integer, ClipSet> mClipSetMap;

    private ClipSetManager() {
        mClipSetMap = new HashMap<>();
    }

    public static final int CLIP_SET_TYPE_ENHANCE = 0x108;
    public static final int CLIP_SET_TYPE_ALLFOOTAGE = 0x109;

    private static volatile ClipSetManager CLIP_SET_MANAGER;

    public static ClipSetManager getManager() {
        if (CLIP_SET_MANAGER == null) {
            synchronized (ClipSetManager.class) {
                if (CLIP_SET_MANAGER == null) {
                    CLIP_SET_MANAGER = new ClipSetManager();
                }
            }
        }
        return CLIP_SET_MANAGER;
    }


    public void updateClipSet(int index, ClipSet clipSet) {
        mClipSetMap.put(index, clipSet);
    }


    public ClipSet getClipSet(int index) {
        return mClipSetMap.get(index);
    }
}
