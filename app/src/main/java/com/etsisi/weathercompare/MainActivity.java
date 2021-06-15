package com.etsisi.weathercompare;

//android

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

//firebase
import com.etsisi.weathercompare.db.AppDataBase;
import com.etsisi.weathercompare.db.ClimaDao;
import com.etsisi.weathercompare.db.ClimaEntity;
import com.etsisi.weathercompare.models.Clima;
import com.etsisi.weathercompare.models.Coord;
import com.etsisi.weathercompare.models.Daily;
import com.etsisi.weathercompare.models.Forecast;
import com.etsisi.weathercompare.models.Main;
import com.etsisi.weathercompare.models.Temp;
import com.etsisi.weathercompare.models.Weather;

//location
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.location.Address;
import android.location.Geocoder;

//retrofit
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


//firebase RealtimeDB

//java
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private boolean locationRequestsEnabled;
    private ClimaAdapter climaAdapter;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private FirebaseAuth mFirebaseAuth;

    private static final String LOG_TAG = "WeatherApp";
    private static final int LOCATION_REQ = 100;
    private static final String API_BASE_URL = "https://api.openweathermap.org";

    private SensorManager sensorManager;
    private Sensor luz, temperature, humedad, presion;

    private TextView maxTemp, maxHumedad, maxPresion, maxLuz, currentCity;
    private TextView mainCityResult, countryResult;
    private EditText etcityName;

    private float lastLuz, lastTemp, lastHumedad, lastPresion;
    private String lat, lon, city, des, tmp, pres, hum;

    private IWeatherRESTAPIService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationRequestsEnabled = TRUE;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        etcityName = (EditText) findViewById(R.id.cityName);
        mainCityResult = (TextView) findViewById(R.id.mainCityResult);
        countryResult = (TextView) findViewById(R.id.countryResult);

        currentCity = (TextView) findViewById(R.id.cityView);
        maxPresion = (TextView) findViewById(R.id.maxPresion);
        maxLuz = (TextView) findViewById(R.id.maxLuz);
        maxTemp = (TextView) findViewById(R.id.maxTemp);
        maxHumedad = (TextView) findViewById(R.id.maxHumedad);

        getLastLocation();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {
            Log.e(LOG_TAG, "Success! we have pressure");
            presion = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            sensorManager.registerListener(this, presion, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.e(LOG_TAG, "Failure. we do not have pressure");
            maxPresion.setText("Cannot get Pressure");
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            Log.e(LOG_TAG, "Success! we have light!");
            luz = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            sensorManager.registerListener(this, luz, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            maxLuz.setText("Cannot get Light");
            Log.e(LOG_TAG, "Failure. we do not have light");
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            Log.e(LOG_TAG, "Success! we have temperature!");
            temperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            sensorManager.registerListener(this, temperature, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            maxTemp.setText("Cannot get temperature");
            Log.e(LOG_TAG, "Failure. we do not have temperature");
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null) {
            Log.e(LOG_TAG, "Success! we have humidity!");
            humedad = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
            sensorManager.registerListener(this, humedad, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            maxHumedad.setText("Cannot get humidity");
            Log.e(LOG_TAG, "Failure. we do not have humidity");
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    getLastLocation();
                }
            }
        };

        //Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(IWeatherRESTAPIService.class);
        initRecyclerView();
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
        climaAdapter = new ClimaAdapter(this);
        recyclerView.setAdapter(climaAdapter);

        loadClima();

    }

    private void getLastLocation() {
        if (ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
                            List<Address> addresses;
                            try {
                                addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                if (addresses.size() > 0) {
                                    currentCity.setText(addresses.get(0).getLocality());
                                    getMainCity(addresses.get(0).getLocality());
                                }
                            } catch (IOException e) {
                                currentCity.setText("No city found.");
                                e.printStackTrace();
                            }
                        } else {
                            currentCity.setText("No location found.");
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    LOCATION_REQ);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQ) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    getLastLocation();
                }
            }
        }
    }

    private void getMainCity(String cCity) {
        Log.i(LOG_TAG, "getCountryByName = " + cCity);
        mainCityResult.setText("");

        //Retrofit call
        Call<Clima> call_async = apiService.getCityByName2(cCity, "metric", "8dedcc7198c51b7d7119a0c408ce01fd");
        call_async.enqueue(new Callback<Clima>() {
            @Override
            public void onResponse(Call<Clima> call, Response<Clima> response) {
                Clima clima = response.body();

                if (null != clima) {
                    List<Weather> weather = clima.getWeather();
                    for (Weather w : weather) {
                        mainCityResult.append("Weather: " + w.getDescription() + "\n\n");
                    }

                    Main mainList = clima.getMain();
                    if (mainList != null) {
                        mainCityResult.append("Temperature: " + mainList.getTemp() + "C \n");
                        mainCityResult.append("Pressure: " + mainList.getPressure() + "hPa \n");
                        mainCityResult.append("Humidity: " + mainList.getHumidity() + "% \n");
                    }
                    stopLocationUpdates();
                    locationRequestsEnabled = FALSE;
                } else {
                    mainCityResult.setText(getString(R.string.strError));
                    Log.i(LOG_TAG, getString(R.string.strError));
                }
            }

            private void stopLocationUpdates() {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                Log.i(LOG_TAG, "Stop location updates");
            }

            @Override
            public void onFailure(Call<Clima> call, Throwable t) {
                Toast.makeText(
                        getApplicationContext(),
                        "ERROR: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
                Log.e(LOG_TAG, t.getMessage());
            }
        });
    }

    private void saveClima(String cityname, String lat, String lon, String desc, String hum, String pres, String tmp) {
        AppDataBase db = AppDataBase.getDbInstance(this.getApplicationContext());
        ClimaEntity climaEntity = new ClimaEntity();
        climaEntity.city_name = cityname;
        climaEntity.latitud = lat;
        climaEntity.longitud = lon;
        climaEntity.descrp = desc;
        climaEntity.humidit = hum;
        climaEntity.pressur = pres;
        climaEntity.temperat = tmp;
        db.climaDao().insertCity(climaEntity);
    }

    private void loadClima() {
        AppDataBase db = AppDataBase.getDbInstance(this.getApplicationContext());
        List<ClimaEntity> climaEntityList = db.climaDao().getAllCities();
        climaAdapter.setClimaEntityList(climaEntityList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, presion, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, luz, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, temperature, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, humedad, SensorManager.SENSOR_DELAY_NORMAL);
        if (locationRequestsEnabled){
            startLocationUpdates();
            Log.i(LOG_TAG, "start location");
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Log.i(LOG_TAG, "Getting looper ready");
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
        getLastLocation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE){
            lastPresion = event.values[0];
            maxPresion.setText(getString(R.string.presion, Float.toString(lastPresion)));
        }

        if (event.sensor.getType() == Sensor.TYPE_LIGHT){
            lastLuz = event.values[0];
            maxLuz.setText(getString(R.string.luz, Float.toString(lastLuz)));
        }

        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
            lastTemp = event.values[0];
            maxTemp.setText(getString(R.string.temperature, Float.toString(lastTemp)));
        }
        if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY){
            lastHumedad = event.values[0];
            maxHumedad.setText(getString(R.string.humedad, Float.toString(lastHumedad)));
        }
        updateSensores();
    }

    private void updateSensores() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("sensores");
        Map<String,Object> m = new HashMap<>();
        m.put("temperature", lastTemp);
        m.put("humidity", lastHumedad);
        m.put("pressure", lastPresion);
        m.put("light", lastLuz);
        myRef.setValue(m);
    }

    public void logOut(View v) {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    public void getData(View v){
        saveClima(city,lat,lon,des,hum,pres,tmp);
        loadClima();
    }

    public  void getForecast(View v) {
        if (lat != null){
            countryResult.setText("");
            countryResult.append("Forecast for " + city + "\n");
            SimpleDateFormat jdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

            //RetroFit
            Call<Forecast> call_async = apiService.getForecast(lat, lon, "metric","current,minutely,hourly,alerts", "8dedcc7198c51b7d7119a0c408ce01fd");
            call_async.enqueue(new Callback<Forecast>() {
                @Override
                public void onResponse(Call<Forecast> call, Response<Forecast> response) {
                    Forecast forecast = response.body();
                    countryResult.append(forecast.getTimezone() + "\n\n");
                    List<Daily> dailyList = forecast.getDaily();
                    for (Daily d : dailyList){

                        Date date = new Date(Long.parseLong(d.getDt().toString()) * 1000);
                        Log.i(LOG_TAG, jdf.format(date));
                        countryResult.append("Date: " + jdf.format(date) +"\n\n");
                        Date sunrise = new Date(Long.parseLong(d.getSunrise().toString()) * 1000);
                        countryResult.append("Sunrise: " + jdf.format(sunrise) +"\n");
                        Date sunset = new Date(Long.parseLong(d.getSunset().toString()) * 1000);
                        countryResult.append("Sunset: " + jdf.format(sunset) +"\n");
                        countryResult.append("Expected Temperature: \n");
                        List<Weather> weather = d.getWeather();
                        for (Weather w: weather){
                            countryResult.append("Day:" + w.getDescription()+ "\n");
                        }

                        Temp t = d.getTemp();
                        countryResult.append("Day:" + t.getDay().toString()+ "\n");
                        countryResult.append("Evening:" + t.getEve().toString() + "\n");
                        countryResult.append("Night:" + t.getNight().toString() + "\n\n");

                    }
                }

                @Override
                public void onFailure(Call<Forecast> call, Throwable t) {
                    Toast.makeText(
                            getApplicationContext(),
                            "ERROR: " + t.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    Log.e(LOG_TAG, t.getMessage());

                }
            });
        }
    }

    private void closeKeyboard()
    {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager manager
                    = (InputMethodManager)
                    getSystemService(
                            Context.INPUT_METHOD_SERVICE);
            manager
                    .hideSoftInputFromWindow(
                            view.getWindowToken(), 0);
        }
    }

    public void getCityByName(View v) {
        closeKeyboard();
        String cityName = etcityName.getText().toString();
        Log.i(LOG_TAG, "getCityByName = " + cityName);
        etcityName.setText("");
        countryResult.setText("No Forecast data to show");

        //Retrofit call
        Call<Clima> call_async = apiService.getCityByName2(cityName,"metric","8dedcc7198c51b7d7119a0c408ce01fd");
        call_async.enqueue(new Callback<Clima>() {
            @Override
            public void onResponse(Call<Clima> call, Response<Clima> response) {
                Clima clima = response.body();

                if (null != clima) {
                    city = clima.getName();
                    Coord coord = clima.getCoord();
                    if (coord != null) {
                        lat = coord.getLat().toString();
                        Log.i(LOG_TAG, lat);
                        lon = coord.getLon().toString();
                        Log.i(LOG_TAG, lon);
                    }

                    List<Weather> weather = clima.getWeather();
                    for (Weather w: weather) {
                        des = w.getDescription();
                    }

                    Main mainList = clima.getMain();
                    if (mainList != null){
                        tmp = mainList.getTemp().toString();
                        pres = mainList.getPressure().toString();
                        hum = mainList.getHumidity().toString();
                    }
                    saveClima(city,lat,lon,des,hum,pres,tmp);
                    loadClima();

                } else {
                    Log.i(LOG_TAG, getString(R.string.strError));
                }
            }

            @Override
            public void onFailure(Call<Clima> call, Throwable t) {
                Toast.makeText(
                        getApplicationContext(),
                        "ERROR: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
                Log.e(LOG_TAG, t.getMessage());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}