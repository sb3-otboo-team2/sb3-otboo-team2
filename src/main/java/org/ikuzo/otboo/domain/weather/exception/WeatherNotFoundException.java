package org.ikuzo.otboo.domain.weather.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;

public class WeatherNotFoundException extends WeatherException {
    public WeatherNotFoundException() {
        super(ErrorCode.WEATHER_NOT_FOUND);
    }
}
