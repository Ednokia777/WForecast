package com.stnsystem.wforecast.data.remote;

import com.stnsystem.wforecast.domain.model.WeatherInfo;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApi {

    @GET("weather")
    Call<WeatherInfo> getCurrentWeather(
            @Query("lat") double latitude,
            @Query("lon") double longitude,
            @Query("exclude") String exclude,
            @Query("appid") String apiKey
    );
}