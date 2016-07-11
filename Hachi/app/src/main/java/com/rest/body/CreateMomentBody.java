package com.rest.body;

import com.waylens.hachi.ui.entities.LocalMoment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.orhanobut.logger.Logger;

import org.json.JSONException;

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
        this.overlay = localMoment.gaugeSettings;

        Logger.d("after overlay setting");

        if (localMoment.audioID > 0) {
            this.audioType = 1;
            this.musicSource = String.valueOf(localMoment.audioID);
        } else {
            this.audioType = 0;
        }

        if (localMoment.isFbShare) {
            shareProviders.add("facebook");
        }

    }
}
