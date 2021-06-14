package com.etsisi.weathercompare;

//android
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

//firebase
import com.etsisi.weathercompare.models.Clima;
import com.etsisi.weathercompare.models.Main;
import com.etsisi.weathercompare.models.Weather;

//location
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String LOG_TAG = "WeatherApp";
    private static final int LOCATION_REQ = 100;
    private static final String API_BASE_URL = "https://api.openweathermap.org";

    private SensorManager sensorManager;
    private Sensor luz, temperature, humedad, presion;

    private float lastLuz, lastTemp, lastHumedad, lastPresion;
    private double lat, lon;

    private TextView maxTemp, maxHumedad, maxPresion, maxLuz, currentCity;

    private TextView countryResult, mainCityResult;
    private EditText etcityName;

    private IWeatherRESTAPIService apiService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        countryResult = (TextView) findViewById(R.id.cityResult);
        etcityName = (EditText) findViewById(R.id.cityName);
        mainCityResult = (TextView) findViewById(R.id.mainCityResult);

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

        //Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(IWeatherRESTAPIService.class);
      //  findViewById(R.id.logoutButton).setOnClickListener(this);
    }

    private void getLastLocation(){
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if ( ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            Geocoder gcd = new Geocoder(getApplicationContext(),Locale.getDefault());
                            List<Address> addresses;
                            try {
                                addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(),1);
                                if (addresses.size() > 0) {
                                    currentCity.setText(getString(R.string.currentcity,addresses.get(0).getLocality()));
                                    getMainCity(addresses.get(0).getLocality());
                                }
                            } catch (IOException e) {
                                currentCity.setText(getString(R.string.currentcity,"No city found."));
                                e.printStackTrace();
                            }
                        } else {
                            currentCity.setText(getString(R.string.currentcity,"No location found."));
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
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQ) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void getMainCity(String cCity){
        Log.i(LOG_TAG, "getCountryByName = " + cCity);
        mainCityResult.setText("");

        //Retrofit call
        Call<Clima> call_async = apiService.getCityByName2(cCity,"metric","8dedcc7198c51b7d7119a0c408ce01fd");
        call_async.enqueue(new Callback<Clima>() {
            @Override
            public void onResponse(Call<Clima> call, Response<Clima> response) {
                Clima clima = response.body();

                if (null != clima) {
                    List<Weather> weather = clima.getWeather();
                    for (Weather w: weather) {
                        mainCityResult.append("Weather: " + w.getDescription()+"\n\n");
                    }

                    Main mainList = clima.getMain();
                    if (mainList != null){
                        mainCityResult.append("Temperature: " + mainList.getTemp() + "C \n\n");
                        mainCityResult.append("Pressure: " + mainList.getPressure() + "hPa \n\n");
                        mainCityResult.append("Humidity: " + mainList.getHumidity() + "% \n\n");
                    }
                } else {
                    mainCityResult.setText(getString(R.string.strError));
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
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this, presion, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, luz, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, temperature, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, humedad, SensorManager.SENSOR_DELAY_NORMAL);
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

 //   @Override
  //  public void onClick(View v) {
        //mFirebaseAuth.signOut();
    //    Log.i(LOG_TAG, getString(R.string.signed_out));
    //}

    public  void getForecast(View v) {


    }

    public void getCityByName(View v) {
        updateSensores();
        String cityName = etcityName.getText().toString();
        Log.i(LOG_TAG, "getCountryByName = " + cityName);
        countryResult.setText("");
        etcityName.setText("");

        //Retrofit call
        Call<Clima> call_async = apiService.getCityByName2(cityName,"metric","8dedcc7198c51b7d7119a0c408ce01fd");
        call_async.enqueue(new Callback<Clima>() {
            @Override
            public void onResponse(Call<Clima> call, Response<Clima> response) {
                Clima clima = response.body();

                if (null != clima) {
                    countryResult.append(clima.getName() + "\n\n");
                    List<Weather> weather = clima.getWeather();
                    for (Weather w: weather) {
                        countryResult.append(" - " + w.getDescription() + "\n\n");
                    }

                    Main mainList = clima.getMain();
                    if (mainList != null){
                        countryResult.append(" - Temp: " + mainList.getTemp() + "C" + "\n\n");
                        countryResult.append(" - Pressure: " + mainList.getPressure() + "hPa" + "\n\n");
                        countryResult.append(" - Humidity: " + mainList.getHumidity() + "%" + "\n\n");
                    }
                } else {
                    countryResult.setText(getString(R.string.strError));
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