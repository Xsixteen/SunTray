package com.ericulicny.weather;

import com.ericulicny.domain.Weather;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherService {
    private String BASE_URL = "http://api.openweathermap.org/data/2.5/weather";
    public WeatherService() {

    }

    public Weather getTodaysWeather(String city, String appId) {
        Weather weather = new Weather();
        try {
            URL url = new URL(BASE_URL + "?q=" + city + "&appid=" +appId );
            HttpURLConnection con = null;
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int status = con.getResponseCode();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();


            System.out.println("Weather API returned = " + status);
            JSONObject obj = new JSONObject(content.toString());
            Double currentTempK = obj.getJSONObject("main").getDouble("temp");
            Double maxTempK = obj.getJSONObject("main").getDouble("temp_max");
            Double minTempK = obj.getJSONObject("main").getDouble("temp_min");
            Double windSpeed = obj.getJSONObject("wind").getDouble("speed");
            try {
                Double windGust = obj.getJSONObject("wind").getDouble("gust");
                weather.setGustWind(windMetertoMph(windGust).intValue());
            } catch ( JSONException jse) {
                jse.printStackTrace();
            }
            weather.setCurrentTempF(convKtoF(currentTempK));
            weather.setMaxTempF(convKtoF(maxTempK));
            weather.setMinTempF(convKtoF(minTempK));
            weather.setCurrentWind(windMetertoMph(windSpeed).intValue());
            weather.setCurrentWeather(obj.getJSONArray("weather").getJSONObject(0).getString("main"));
            System.out.println("Received Weather: current:" + weather.getCurrentTempF() + " max:" + weather.getMaxTempF() + " min:"+ weather.getMinTempF());
            return weather;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Long convKtoF(Double kelvin) {
        return (Math.round(kelvin - 273.15) * (9/5) + 32);
    }

    private Double windMetertoMph(Double windSpeed) {
        return windSpeed * 2.23694;
    }
}
