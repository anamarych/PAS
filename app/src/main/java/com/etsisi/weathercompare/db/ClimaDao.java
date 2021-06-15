package com.etsisi.weathercompare.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ClimaDao {

    @Query("SELECT * FROM climaentity")
    List<ClimaEntity> getAllCities();

    @Insert
    void insertCity(ClimaEntity... climaEntities);

    @Delete
    void delete(ClimaEntity climaEntity);
}
