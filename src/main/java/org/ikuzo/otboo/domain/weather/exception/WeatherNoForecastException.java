package org.ikuzo.otboo.domain.weather.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;

public class WeatherNoForecastException extends WeatherException {
    public WeatherNoForecastException() {
        super(ErrorCode.WEATHER_NO_FORECAST);
    }

    public static WeatherNoForecastException withBaseAndGrid(String baseDate, String baseTime, int x, int y) {
        WeatherNoForecastException ex = new WeatherNoForecastException();
        ex.addDetail("baseDate", baseDate);
        ex.addDetail("baseTime", baseTime);
        ex.addDetail("nx", x);
        ex.addDetail("ny", y);
        return ex;
    }

    public static WeatherNoForecastException withLatLonAndBase(double lat, double lon, String baseDate,
                                                               String baseTime) {
        WeatherNoForecastException ex = new WeatherNoForecastException();
        ex.addDetail("latitude", lat);
        ex.addDetail("longitude", lon);
        ex.addDetail("baseDate", baseDate);
        ex.addDetail("baseTime", baseTime);
        return ex;
    }
}