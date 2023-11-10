package com.stnsystem.wforecast.domain.repository;

import com.stnsystem.wforecast.data.remote.WeatherApi;
import com.stnsystem.wforecast.domain.model.WeatherInfo;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherRepository {

    private final WeatherApi weatherApi;

    public WeatherRepository(WeatherApi weatherApi) {
        this.weatherApi = weatherApi;
    }

    public Observable<WeatherInfo> fetchWeatherData(double latitude, double longitude) {
        return Observable.create(emitter -> {
            Call<WeatherInfo> call = weatherApi.getCurrentWeather(latitude, longitude, "exclude=hourly,daily", "803cbe747d7ede299f0c3bf511fc0231");
            call.enqueue(new Callback<WeatherInfo>() {
                @Override
                public void onResponse(Call<WeatherInfo> call, Response<WeatherInfo> response) {
                    if (response.isSuccessful()) {
                        emitter.onNext(response.body());
                        emitter.onComplete();
                    } else {
                        emitter.onError(new Exception(response.message()));
                    }
                }

                @Override
                public void onFailure(Call<WeatherInfo> call, Throwable t) {
                    emitter.onError(t);
                }
            });
        });
    }
}
