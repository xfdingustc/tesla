package com.waylens.hachi.utils;

import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Xiaofei on 2016/4/20.
 */
public class ClipSetGroupHelper {
    private final ClipSet mClipSet;

    private Map<String, ClipSet> mClipSetGroup = new HashMap<>();

    public ClipSetGroupHelper(ClipSet clipSet) {
        this.mClipSet = clipSet;
    }

    public List<ClipSet> getClipSetGroup() {
        calculateClipSetGroup(mClipSet);

        List<ClipSet> clipSetGroup = new ArrayList<>();
        Iterator iter = mClipSetGroup.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            clipSetGroup.add((ClipSet) entry.getValue());
        }

        Collections.sort(clipSetGroup, new Comparator<ClipSet>() {
            @Override
            public int compare(ClipSet lhs, ClipSet rhs) {
                return (int)(rhs.getClip(0).getClipDate() - lhs.getClip(0).getClipDate());
            }
        });

        return clipSetGroup;
    }

    private void calculateClipSetGroup(ClipSet clipSet) {
        for (Clip clip : clipSet.getClipList()) {

            String clipDataString = clip.getDateString();
            ClipSet oneClipSet = mClipSetGroup.get(clipDataString);
            if (oneClipSet == null) {
                oneClipSet = new ClipSet(clipSet.getType());
                mClipSetGroup.put(clipDataString, oneClipSet);
            }

            oneClipSet.addClip(clip);

        }
    }


}
