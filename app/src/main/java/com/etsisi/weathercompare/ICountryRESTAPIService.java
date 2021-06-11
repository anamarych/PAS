package com.etsisi.weathercompare;

import java.util.List;

import com.etsisi.weathercompare.models.Clima;
import com.etsisi.weathercompare.models.Weather;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ICountryRESTAPIService {
    //getCity
    @GET("weather?q={countryName}")
    Call<List<Weather>> getWeather(@Path("countryName") String countryName, @Query("APPID") String keytoken);
}
