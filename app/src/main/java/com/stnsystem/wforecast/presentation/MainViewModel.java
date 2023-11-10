package com.stnsystem.wforecast.presentation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.stnsystem.wforecast.domain.model.WeatherInfo;
import com.stnsystem.wforecast.domain.repository.WeatherRepository;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;



public class MainViewModel extends AndroidViewModel {

    private final WeatherRepository weatherRepository;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final MutableLiveData<WeatherInfo> weatherData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application, WeatherRepository weatherRepository) {
        super(application);
        this.weatherRepository = weatherRepository;
        // other initialization...
    }

    public LiveData<WeatherInfo> getWeatherData() {
        return weatherData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void fetchWeatherData(double latitude, double longitude) {
        compositeDisposable.add(
                weatherRepository.fetchWeatherData(latitude, longitude)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                weatherInfo -> weatherData.setValue(weatherInfo),
                                throwable -> errorMessage.setValue(throwable.getMessage())
                        )
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Dispose of RxJava subscriptions when the ViewModel is no longer used
        compositeDisposable.clear();
    }
}
