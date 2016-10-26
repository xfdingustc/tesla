package com.waylens.hachi.ui.entities;



import android.text.TextUtils;

import com.waylens.hachi.snipe.utils.ToStringUtils;

import java.io.Serializable;

/**
 * Created by Xiaofei on 2016/9/22.
 */

public class MomentPicture implements Serializable{
    public long pictureID;
    public String original;
    public String smallThumbnail;
    public String bigThumbnail;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }

    public String getMomentPicturlUrl() {
        if (!TextUtils.isEmpty(bigThumbnail)) {
            return bigThumbnail;
        } else if (!TextUtils.isEmpty(smallThumbnail)) {
            return smallThumbnail;
        } else {
            return original;
        }
    }
}
