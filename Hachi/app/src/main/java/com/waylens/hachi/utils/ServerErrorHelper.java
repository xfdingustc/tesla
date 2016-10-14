package com.waylens.hachi.utils;

import android.support.design.widget.Snackbar;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.waylens.hachi.rest.response.ErrorMessageResponse;

import java.io.IOException;

import retrofit2.adapter.rxjava.HttpException;

/**
 * Created by Xiaofei on 2016/10/13.
 */

public class ServerErrorHelper {
    public static void showErrorMessage(View view, Throwable error) {
        if (error instanceof HttpException) {
            HttpException httpException = (HttpException)error;
            try {
                String errorMessage = httpException.response().errorBody().string();
                Gson gson = new GsonBuilder().create();
                ErrorMessageResponse response = gson.fromJson(errorMessage, ErrorMessageResponse.class);
                Snackbar.make(view, response.msg, Snackbar.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
