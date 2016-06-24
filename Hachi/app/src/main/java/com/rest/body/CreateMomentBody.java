package com.rest.body;

import com.waylens.hachi.ui.entities.LocalMoment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Xiaofei on 2016/6/17.
 */
public class CreateMomentBody {
    public String title;

    public String desc;

    public List<String> hashTags;

    public String accessLevel;

    public Map<String, String> overlay;

    public int audioType;

    public String musicSource;

    public List<String> shareProviders = new ArrayList<>();

    public CreateMomentBody(LocalMoment localMoment) {
        this.title = localMoment.title;
        this.desc = localMoment.description;
        this.accessLevel = localMoment.accessLevel;
        if (localMoment.isFbShare) {
            shareProviders.add("facebook");
        }

    }
}
