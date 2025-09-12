package org.ikuzo.otboo.domain.weather.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherDto {
    private UUID id;

    private Instant forecastedAt; // 예보 산출 시각
    private Instant forecastAt;   // 예보 대상 시각

    private WeatherAPILocation location;

    private SkyStatus skyStatus;          // CLEAR / MOSTLY_CLOUDY / CLOUDY
    private PrecipitationDto precipitation;
    private HumidityDto humidity;
    private TemperatureDto temperature;
    private WindSpeedDto windSpeed;
}