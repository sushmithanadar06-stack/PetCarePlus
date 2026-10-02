package com.example.petcareplus.utils;

import android.os.Handler;
import android.os.Looper;

import com.example.petcareplus.models.WeatherInfo;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

public class WeatherFetcher {

    // Mumbai coordinates
    private static final double LATITUDE = 19.0760;
    private static final double LONGITUDE = 72.8777;

    public interface WeatherCallback {
        void onSuccess(WeatherInfo weather);
        void onFailure(String errorMessage);
    }

    public static void fetchMumbaiWeather(WeatherCallback callback) {

        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + LATITUDE
                + "&longitude=" + LONGITUDE
                + "&current=temperature_2m,relative_humidity_2m,precipitation,weather_code"
                + "&timezone=Asia%2FKolkata";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (!response.isSuccessful() || response.body() == null) {
                    mainHandler.post(() -> callback.onFailure("Weather service unavailable"));
                    return;
                }

                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONObject current = json.getJSONObject("current");

                    double temp = current.getDouble("temperature_2m");
                    double humidity = current.getDouble("relative_humidity_2m");
                    double precipitation = current.optDouble("precipitation", 0);
                    int weatherCode = current.optInt("weather_code", 0);

                    boolean isRaining = precipitation > 0 || (weatherCode >= 51 && weatherCode <= 82);

                    String condition;
                    if (isRaining) condition = "Rainy";
                    else if (temp >= 33) condition = "Hot";
                    else if (temp <= 20) condition = "Cool";
                    else condition = "Pleasant";

                    WeatherInfo info = new WeatherInfo(temp, humidity, isRaining, condition);

                    mainHandler.post(() -> callback.onSuccess(info));

                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure("Failed to read weather data"));
                }
            }
        });
    }
}