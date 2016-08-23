package com.waylens.hachi.rest.body;

import com.waylens.hachi.ui.entities.LocalMoment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.orhanobut.logger.Logger;

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

    public String vehicleMaker;

    public String vehicleModel;

    public String vehicleYear;

    public String vehicleDesc;

    public List<Long> timingPoints = new ArrayList<>();

    public String MomentType;

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
            shareProviders.add(SocialProvider.FACEBOOK);
        }

        if (localMoment.isYoutubeShare) {
            shareProviders.add(SocialProvider.YOUTUBE);
        }

    }
}
