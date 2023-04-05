package com.example.weatherapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import android.Manifest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "a87082bc2a12126498886d356718bcf1";
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric";

    private static final int PERMISSION_REQUEST_CODE = 1;
    private Location location;
    private TextView currentCityText, humidityText, windSpeedText, temperatureText;

    double temperatureCelsius;
    int humidityPercentage;
    double windSpeedMetersPerSecond;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentCityText = findViewById(R.id.currentCityText);
        humidityText = findViewById(R.id.humidityText);
        windSpeedText = findViewById(R.id.windSpeedText);
        temperatureText = findViewById(R.id.temperatureText);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If the app doesn't have permission, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            // If the app already has permission, get the location
            getLocation();

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // If the permission was granted, get the location
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                // If the permission was denied, show an error message
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void getLocation() {
        // Get the user's current location
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // If the location is not null, get the city using Geocoder
                            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                            try {
                                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                String city = addresses.get(0).getLocality();
                                currentCityText.setText(city);
                                new GetWeatherDataTask().execute(city);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    private class GetWeatherDataTask extends AsyncTask<String, Void, WeatherData> {

        WeatherData weatherData = new WeatherData();

        @Override
        protected WeatherData doInBackground(String... params) {
            String cityName = params[0];
            String url = String.format(API_URL, cityName, API_KEY);

            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {

                                JSONObject main = response.getJSONObject("main");
                                weatherData.setTemperature(main.getDouble("temp"));
                                weatherData.setHumidity(main.getDouble("humidity"));

                                JSONObject wind = response.getJSONObject("wind");
                                weatherData.setWindSpeed(wind.getDouble("speed"));

                                onPostExecute(weatherData);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("ERROR", error.toString());
                        }
                    });

            queue.add(jsonObjectRequest);

            return null;
        }

        @Override
        protected void onPostExecute(WeatherData weatherData) {
            super.onPostExecute(weatherData);
            weatherData = this.weatherData;
            temperatureText.setText(String.format("%.1f °C", weatherData.getTemperature()));
            humidityText.setText(String.format("%.1f%%", weatherData.getHumidity()));
            windSpeedText.setText(String.format("%.1f m/s", weatherData.getWindSpeed()));
        }
    }

}