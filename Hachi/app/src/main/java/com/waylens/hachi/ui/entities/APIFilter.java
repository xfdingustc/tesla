package com.waylens.hachi.ui.entities;

import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import java.util.ArrayList;

/**
 * Created by Richard on 9/8/15.
 */
public class APIFilter implements RequestQueue.RequestFilter {

    String mAPI;
    ArrayList<String> mAPIList;

    public APIFilter(String api) {
        mAPI = api;
    }

    public APIFilter(ArrayList<String> apis) {
        mAPIList = new ArrayList<>();
        mAPIList.addAll(apis);
    }


    @Override
    public boolean apply(Request<?> request) {
        if (mAPI != null
                && request.getOriginUrl().startsWith(mAPI)) {
            return true;
        }

        if (mAPIList != null) {
            for (String api : mAPIList) {
                if (request.getOriginUrl().startsWith(api)) {
                    return true;
                }
            }
        }
        return false;
    }
}
