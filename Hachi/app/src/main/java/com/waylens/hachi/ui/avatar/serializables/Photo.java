package com.waylens.hachi.ui.avatar.serializables;

import java.io.Serializable;

/**
 * Created by Xiaofei on 2015/6/29.
 */
public class Photo implements Serializable {

    private int ImageId;
    private String mUrl;
    private String PathAbsolute;

    public Photo() {
    }

    public Photo(int imageId, String url, String pathAbsolute) {
        super();
        ImageId = imageId;
        this.mUrl = url;
        PathAbsolute = pathAbsolute;
    }


    public int getImageId() {
        return ImageId;
    }

    public void setImageId(int image_id) {
        this.ImageId = image_id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public String getPathAbsolute() {
        return PathAbsolute;
    }

    public void setPathAbsolute(String path_absolute) {
        this.PathAbsolute = path_absolute;
    }
}
