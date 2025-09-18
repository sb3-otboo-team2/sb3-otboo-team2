package org.ikuzo.otboo.domain.weather.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.weather.dto.WeatherAPILocation;
import org.ikuzo.otboo.domain.weather.dto.WeatherDto;
import org.ikuzo.otboo.domain.weather.service.WeatherReadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weathers")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherReadService weatherReadService;

    @GetMapping
    public ResponseEntity<List<WeatherDto>> getWeathers(
        @RequestParam double latitude,
        @RequestParam double longitude
    ) {
        return ResponseEntity.ok(weatherReadService.getWeatherByCoordinates(latitude, longitude));
    }

    @GetMapping("/location")
    public ResponseEntity<WeatherAPILocation> getLocation(
        @RequestParam double latitude,
        @RequestParam double longitude
    ) {
        return ResponseEntity.ok(weatherReadService.getLocation(latitude, longitude));
    }
}