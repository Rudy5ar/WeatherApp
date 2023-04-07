package com.example.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "a87082bc2a12126498886d356718bcf1";
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric";
    private static final String API_URL_FORECAST = "https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric";

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int CAMERA_REQUEST_CODE = 2;
    private EditText input;
    public static RecyclerView recyclerView;
    private TextView currentCityText, humidityText, windSpeedText, temperatureText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentCityText = findViewById(R.id.currentCityText);
        humidityText = findViewById(R.id.humidityText);
        windSpeedText = findViewById(R.id.windSpeedText);
        temperatureText = findViewById(R.id.temperatureText);
        recyclerView = findViewById(R.id.forecastRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        input = new EditText(this);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // No permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            // Have permission
            getLocation(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.searchItem:
                String city = showSearchDialog();
                return true;
            case R.id.cameraItem:
                openCamera();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String showSearchDialog() {

        final String[] city = {""};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search");

        if (input.getParent() != null) {
            ((ViewGroup)input.getParent()).removeView(input);
            input.setText("");
        }

        builder.setView(input);
        builder.setPositiveButton("Search", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String searchText = input.getText().toString();
                city[0] = searchText;
                getLocation(city[0]);

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
        return city[0];
    }


    public void openCamera(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");

            @SuppressLint("SimpleDateFormat") String fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg";
            File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File imageFile = new File(picturesDirectory, fileName);

            try {
                FileOutputStream outputStream = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.flush();
                outputStream.close();

                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(imageFile));
                sendBroadcast(intent);

                Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save image to gallery", Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation(null);
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    private class GetForecastTask extends AsyncTask<String, Void, List<ForecastItem>> {
        List<ForecastItem> data = new ArrayList<>();

        @Override
        protected List<ForecastItem> doInBackground(String... params) {
            String cityName = params[0];
            String url = String.format(API_URL_FORECAST, cityName, API_KEY);
            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            data = getForecastForCity(cityName);
                            onPostExecute(data);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("ERROR", error.toString());
                        }
                    });

            queue.add(jsonObjectRequest);

            return data;

        }

        @Override
        protected void onPostExecute(List<ForecastItem> data) {
            super.onPostExecute(data);
            this.data = data;

        }
    }

    public void updateUI(List<ForecastItem> data){
        ForecastAdapter adapter = new ForecastAdapter(data);
        recyclerView.setAdapter(adapter);
        Log.d("size", String.valueOf(data.size()));
    }

    public List<ForecastItem> getForecastForCity(String cityName) {
        List<ForecastItem> data = new ArrayList<>();

        String url = "https://api.openweathermap.org/data/2.5/forecast?q=" + cityName + "&appid=" + API_KEY + "&units=metric";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONArray forecastList = response.getJSONArray("list");

                            for (int i = 0; i < 5; i++) {

                                JSONObject forecastData = forecastList.getJSONObject(i);

                                String timeString = forecastData.getString("dt_txt");
                                timeString = timeString.substring(11);
                                JSONObject main = forecastData.getJSONObject("main");
                                String temperature = String.valueOf(main.getDouble("temp"));
                                Log.d("temp", temperature);

                                ForecastItem fi = new ForecastItem(timeString, temperature);
                                data.add(fi);
                            }
                            updateUI(data);
                        } catch (JSONException e) {
                            Log.d("firstError", e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("secondError", error.getMessage());

                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // add the request to the request queue
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        requestQueue.add(request);
        return data;
    }

    @SuppressLint("MissingPermission")
    public void getLocation(String cityName) {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if(cityName == null){
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    String city = addresses.get(0).getLocality();
                                    currentCityText.setText(city);
                                    new GetWeatherDataTask().execute(city);
                                    new GetForecastTask().execute(city);

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
        } else {
            try {
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(cityName, 1);
                currentCityText.setText(cityName);
                if (addresses != null && !addresses.isEmpty()) {

                    new GetWeatherDataTask().execute(cityName);
                    new GetForecastTask().execute(cityName);


                } else {
                    Toast.makeText(this, "The city you entered doesn't exist", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
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