package com.etsisi.weathercompare.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ClimaEntity {

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "city_name")
    public String city_name;

    @ColumnInfo(name = "latitud")
    public String latitud;

    @ColumnInfo(name = "longitud")
    public String longitud;

    @ColumnInfo(name = "desciption")
    public String descrp;

    @ColumnInfo(name = "temperature")
    public String temperat;

    @ColumnInfo(name = "humidity")
    public String humidit;

    @ColumnInfo(name = "pressure")
    public String pressur;

}
