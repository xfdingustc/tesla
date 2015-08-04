package com.waylens.network;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Weather service
 * Created by liangyx on 7/21/15.
 */
public interface WeatherService {
    //q=31.190979000000002%2C121.60145658333334&format=json&num_of_days=1&tp=12&key=e081e88edf6ffe4bcd0d12f34b26e
    @GET("/free/v2/weather.ashx")
    public void getWeather(@Query("key") String key,
                           @Query("format") String format,
                           @Query("num_of_days") String numOfDays,
                           @Query("tp") String tp,
                           @Query("q") String gps, Callback<Response> callback);
}
