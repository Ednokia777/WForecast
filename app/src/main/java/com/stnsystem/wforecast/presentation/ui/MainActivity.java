package com.stnsystem.wforecast.presentation.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.stnsystem.wforecast.R;
import com.stnsystem.wforecast.data.remote.RetrofitClient;
import com.stnsystem.wforecast.data.remote.WeatherApi;
import com.stnsystem.wforecast.databinding.ActivityMainBinding;
import com.stnsystem.wforecast.data.local.AppDatabase;
import com.stnsystem.wforecast.data.local.WeatherEntity;
import com.stnsystem.wforecast.domain.model.WeatherInfo;
import com.stnsystem.wforecast.domain.repository.WeatherRepository;
import com.stnsystem.wforecast.presentation.MainViewModel;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    private MainViewModel mainViewModel;
    WeatherApi weatherApi = RetrofitClient.create();
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123; // You can use any value you like
    private static final double KELVIN = 273.15; // You can use any value you like
    private LottieAnimationView lottieAnimationView;

    private AppDatabase appDatabase;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WeatherRepository weatherRepository = new WeatherRepository(RetrofitClient.create());
        MainViewModelFactory factory = new MainViewModelFactory(getApplication(), new WeatherRepository(RetrofitClient.create()));
        mainViewModel = new ViewModelProvider(this, factory).get(MainViewModel.class);
        // Observe LiveData
        mainViewModel.getWeatherData().observe(this, this::handleWeatherData);
        mainViewModel.getErrorMessage().observe(this, this::showToast);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        lottieAnimationView = binding.animationView;
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                fetchWeatherData(latitude, longitude);
                stopLocationUpdates();
            }
        };
        setWeather();
        appDatabase = AppDatabase.getDatabase(this);
    }

    private void fetchWeatherDataIfPermissionGranted() {
        // Use ViewModel method to fetch data
        if (checkLocationPermission()) {
            getLocationObservable()
                    .flatMap(location -> {
                        mainViewModel.fetchWeatherData(location.getLatitude(), location.getLongitude());
                        return Observable.empty(); // You might want to emit something else here
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        }
    }

    private void handleWeatherData(WeatherInfo weatherData) {
        double temperatureKelvin = weatherData.getMain().getTemp();
        int temperatureCelsius = (int) convertToCelsius(temperatureKelvin);

        binding.temperatureTv.setText(String.format(temperatureCelsius + " °C"));
        binding.cityTv.setText(String.valueOf(weatherData.getName()));
        setImage(weatherData.getWeather()[0].getMain());

        WeatherEntity weatherEntity = new WeatherEntity();
        weatherEntity.setCityName(weatherData.getName());
        weatherEntity.setTemperatureCelsius(temperatureCelsius);

        // Perform database operation on a background thread
        Completable.fromAction(() -> appDatabase.weatherDao().insertWeather(weatherEntity))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                }, throwable -> {
                    showToast(throwable.getMessage());
                });
    }

    private Observable<Location> getLocationObservable() {
        return Observable.create(emitter -> {
            if (checkLocationPermission()) {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(location -> {
                            if (location != null) {
                                emitter.onNext(location);
                                emitter.onComplete();
                            } else {
                                emitter.onError(new Exception("Location is null"));
                            }
                        })
                        .addOnFailureListener(emitter::onError);
            } else {
                emitter.onError(new SecurityException("Location permission not granted"));
            }
        });
    }

    private void setWeather() {
        fetchWeatherDataIfPermissionGranted();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // Set the update interval in milliseconds
        locationRequest.setFastestInterval(5000); // The fastest interval for updates
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (checkLocationPermission()) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
        weatherApi = RetrofitClient.create();
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                showPermissionDeniedMessage();
            }
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }
    private void showPermissionDeniedMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location Permission Denied");
        builder.setMessage("This app requires location permission to function. Please grant the permission in the app settings.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle the user's response as needed
            }
        });
        builder.show();
    }

    private Observable<WeatherInfo> fetchWeatherData(double latitude, double longitude) {
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

    private void setImage(String main) {
        Log.d("KURAPIZDA", main);
        switch (main) {
            case "Clear":
                lottieAnimationView.setAnimation(R.raw.clear);
                break;
            case "Thunderstorm":
                lottieAnimationView.setAnimation(R.raw.thunderstorm);
                break;
            case "Drizzle":
                lottieAnimationView.setAnimation(R.raw.drizzle);
                break;
            case "Rain":
                lottieAnimationView.setAnimation(R.raw.rain);
                break;
            case "Atmosphere":
                lottieAnimationView.setAnimation(R.raw.clear);
                break;
            case "Cloud":
                lottieAnimationView.setAnimation(R.raw.cloud);
                break;
            default:
                lottieAnimationView.setAnimation(R.raw.cloud);
                break;
        }
        Log.d("KURAMAIN",  main);
        lottieAnimationView.playAnimation();
        binding.temperatureTv.setVisibility(View.VISIBLE);

    }
    private double convertToCelsius(double temperatureKelvin) {
        return temperatureKelvin - KELVIN;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding= null;
    }
}
