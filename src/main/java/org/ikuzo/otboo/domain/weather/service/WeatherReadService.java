package org.ikuzo.otboo.domain.weather.service;

import java.util.List;
import org.ikuzo.otboo.domain.weather.dto.WeatherAPILocation;
import org.ikuzo.otboo.domain.weather.dto.WeatherDto;

public interface WeatherReadService {
    WeatherAPILocation getLocation(double latitude, double longitude);

    List<WeatherDto> getWeatherByCoordinates(double latitude, double longitude);
}
