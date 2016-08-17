package com.waylens.hachi.rest.response;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Xiaofei on 2016/8/17.
 */
public class MusicCategoryResponse {
    public long lastUpdateTime;

    public List<MusicCategory> categories;


    public static class MusicCategory implements Serializable {
        public long id;
        public String category;
    }

}
