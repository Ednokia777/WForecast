package com.stnsystem.wforecast.data.local;

import androidx.room.Dao;
import androidx.room.Insert;

@Dao
public interface WeatherDao {
    @Insert
    void insertWeather(WeatherEntity weatherEntity);
}