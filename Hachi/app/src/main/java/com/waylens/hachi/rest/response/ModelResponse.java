package com.waylens.hachi.rest.response;
import java.util.List;

/**
 * Created by laina on 16/9/19.
 */
public class ModelResponse {

    public List<Model> models;
    public class Model {
        public long modelID;
        public String modelName;
    }
}
