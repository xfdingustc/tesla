package com.waylens.hachi.rest.body;

import android.os.LocaleList;
import android.text.TextUtils;

import com.waylens.hachi.ui.entities.LocalMoment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.orhanobut.logger.Logger;
import com.xfdingustc.snipe.utils.ToStringUtils;

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

    public String vehicleMaker = null;

    public String vehicleModel = null;

    public int vehicleYear;

    public String vehicleDesc = null;

    public TimingPointsList timingPoints = null;

    public String momentType = null;

    public CreateMomentBody(LocalMoment localMoment) {
        this.title = localMoment.title;
        this.desc = localMoment.description;
        this.accessLevel = localMoment.accessLevel;
        this.overlay = localMoment.gaugeSettings;

        if (!TextUtils.isEmpty(localMoment.momentType) && localMoment.momentType.equals("RACING")) {
            momentType = "RACING";
            timingPoints = new TimingPointsList();
            for (long t:localMoment.mTimingPoints) {
                Logger.d(t);
            }
            timingPoints.t1 = localMoment.mTimingPoints.get(0);
            timingPoints.t2 = localMoment.mTimingPoints.get(1);
            timingPoints.t3 = localMoment.mTimingPoints.get(2);
            timingPoints.t4 = localMoment.mTimingPoints.get(3);
            timingPoints.t5 = localMoment.mTimingPoints.get(4);
            timingPoints.t6 = localMoment.mTimingPoints.get(5);
        }
        if (localMoment.mVehicleMaker != null) {
            vehicleMaker = localMoment.mVehicleMaker;
            vehicleModel = localMoment.mVehicleModel;
            vehicleYear = localMoment.mVehicleYear;
        }
        vehicleDesc = localMoment.mVehicleDesc;

//        Logger.d("after overlay setting");

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

    public class TimingPointsList {
        public long t1;
        public long t2;
        public long t3;
        public long t4;
        public long t5;
        public long t6;
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
