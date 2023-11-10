package com.stnsystem.wforecast.presentation.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.stnsystem.wforecast.domain.repository.WeatherRepository;
import com.stnsystem.wforecast.presentation.MainViewModel;

public class MainViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final WeatherRepository weatherRepository;

    public MainViewModelFactory(Application application, WeatherRepository weatherRepository) {
        this.application = application;
        this.weatherRepository = weatherRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(application, weatherRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}





