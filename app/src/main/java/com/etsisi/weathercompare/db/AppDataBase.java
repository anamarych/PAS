package com.etsisi.weathercompare.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ClimaEntity.class}, version = 1)
public abstract class AppDataBase extends RoomDatabase {

    public abstract ClimaDao climaDao();

    private static AppDataBase INSTANCE;

    public static  AppDataBase getDbInstance(Context context){
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDataBase.class, "DB_CLIMA")
                    .allowMainThreadQueries()
                    .build();

        }
        return INSTANCE;
    }
}
