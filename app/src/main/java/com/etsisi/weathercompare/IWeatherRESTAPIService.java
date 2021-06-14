package com.etsisi.weathercompare;

import com.etsisi.weathercompare.models.Clima;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

interface IWeatherRESTAPIService {
    //https://api.openweathermap.org/data/2.5/weather?q=Madrid&APPID=8dedcc7198c51b7d7119a0c408ce01fd

    @GET("/data/2.5/weather")
    Call<List<Clima>> getCityByName(@Query("q") String cityName, @Query("APPID") String appid);

    @GET("/data/2.5/weather")
    Call<Clima> getCityByName2(@Query("q") String cityName, @Query("units") String unit, @Query("APPID") String appid);

    //https://api.openweathermap.org/data/2.5/onecall?lat=40.4165&lon=-3.7026&exclude=minutely,hourly&appid=8dedcc7198c51b7d7119a0c408ce01fd
    @GET("/data/2.5/onecall")
    Call<Clima> getForecast(@Query("lat") String latitude, @Query("lon") String longitud, @Query("units") String unit, @Query("exclude") String excluded, @Query("APPID") String appid);

}
