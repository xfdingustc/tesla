package com.waylens.hachi.rest.response;

import java.util.List;

/**
 * Created by laina on 16/9/19.
 */
public class MakerResponse {

    public List<Maker> makers;
    public class Maker{
        public long makerID;
        public String makerName;
    }
}
