package com.waylens.hachi.rest.body;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/7/27.
 */
public class RepostBody {
    public long momentID;

    public List<String> shareProviders = new ArrayList<>();

    public RepostBody(long momentID, String provider) {
        this.momentID = momentID;
        this.shareProviders.add(provider);
    }
}
