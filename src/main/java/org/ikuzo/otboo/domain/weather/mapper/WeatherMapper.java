package org.ikuzo.otboo.domain.weather.mapper;

import org.ikuzo.otboo.domain.weather.dto.HumidityDto;
import org.ikuzo.otboo.domain.weather.dto.PrecipitationDto;
import org.ikuzo.otboo.domain.weather.dto.PrecipitationType;
import org.ikuzo.otboo.domain.weather.dto.SkyStatus;
import org.ikuzo.otboo.domain.weather.dto.TemperatureDto;
import org.ikuzo.otboo.domain.weather.dto.WeatherDto;
import org.ikuzo.otboo.domain.weather.dto.WindSpeedDto;
import org.ikuzo.otboo.domain.weather.dto.WindWord;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WeatherMapper {
    // windSpeed 사용하지 않아 임시로 ignore = true
    @Mapping(target = "windSpeed", ignore = true)
    WeatherDto toDto(Weather weather);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "windSpeed", ignore = true)
    Weather toEntity(WeatherDto dto);

    @Named("feedWeatherDto")
    default WeatherDto toFeedWeatherDto(Weather weather) {
        if (weather == null) {
            return null;
        }

        PrecipitationDto precipitationDto = PrecipitationDto.builder()
            .type(parseEnum(PrecipitationType.class, weather.getPrecipitationType(), PrecipitationType.NONE))
            .amount(weather.getPrecipitationAmount())
            .probability(toProbability(weather.getPrecipitationProbability()))
            .build();

        TemperatureDto temperatureDto = TemperatureDto.builder()
            .current(weather.getTemperatureCurrent())
            .comparedToDayBefore(weather.getTemperatureCompared())
            .min(weather.getTemperatureMin())
            .max(weather.getTemperatureMax())
            .build();

        HumidityDto humidityDto = HumidityDto.builder()
            .current(weather.getHumidityCurrent())
            .comparedToDayBefore(weather.getHumidityCompared())
            .build();

        WindSpeedDto windDto = WindSpeedDto.builder()
            .speed(weather.getWindSpeed())
            .asWord(parseEnum(WindWord.class, weather.getWindSpeedWord(), null))
            .build();

        return WeatherDto.builder()
            .id(weather.getId())
            .forecastedAt(weather.getForecastedAt())
            .forecastAt(weather.getForecastAt())
            .location(null)
            .skyStatus(parseEnum(SkyStatus.class, weather.getSkyStatus(), null))
            .precipitation(precipitationDto)
            .humidity(humidityDto)
            .temperature(temperatureDto)
            .windSpeed(windDto)
            .build();
    }

    private Double toProbability(Double percent) {
        if (percent == null) {
            return null;
        }
        return percent / 100.0;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, E defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }
}
