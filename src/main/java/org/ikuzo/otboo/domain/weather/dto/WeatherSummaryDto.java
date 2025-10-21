package org.ikuzo.otboo.domain.weather.dto;

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
public class WeatherSummaryDto {
    private UUID weatherId;
    private SkyStatus skyStatus;
    private PrecipitationDto precipitation;
    private TemperatureDto temperature;
}