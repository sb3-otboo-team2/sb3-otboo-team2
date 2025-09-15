package org.ikuzo.otboo.domain.weather.service;


import java.util.UUID;
import org.ikuzo.otboo.domain.weather.dto.RegionInfoDto;
import org.ikuzo.otboo.domain.weather.dto.WeatherDto;

public interface WeatherService {

    RegionInfoDto reverseGeocode(double latitude, double longitude);

    WeatherDto collectAndSaveForUser(UUID userId);
}
