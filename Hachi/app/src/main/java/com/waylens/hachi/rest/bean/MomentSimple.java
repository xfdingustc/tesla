package com.waylens.hachi.rest.bean;

import android.text.TextUtils;

import com.waylens.hachi.snipe.utils.ToStringUtils;
import com.waylens.hachi.ui.entities.MomentPicture;

import java.util.List;

/**
 * Created by Xiaofei on 2016/10/14.
 */

public class MomentSimple {
    public long momentID;
    public String momentType;
    public String title;
    public String provider;
    public String videoThumbnail;
    public String videoID;
    public List<MomentPicture> pictureInfo;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }

    public boolean isPictureMoment() {
        return !TextUtils.isEmpty(momentType) && momentType.equals("PICTURE");
    }

    public String getMomentThumbnail() {
        if (isPictureMoment()) {
            if (pictureInfo != null && !pictureInfo.isEmpty()) {
                return pictureInfo.get(0).getMomentPicturlUrl();
            }
        } else {
            return videoThumbnail;
        }

        return null;
    }


}
