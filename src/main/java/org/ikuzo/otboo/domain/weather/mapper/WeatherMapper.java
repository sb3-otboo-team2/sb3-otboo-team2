package org.ikuzo.otboo.domain.weather.mapper;

import org.ikuzo.otboo.domain.weather.dto.WeatherDto;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WeatherMapper {
    // windSpeed 사용하지 않아 임시로 ignore = true
    @Mapping(target = "windSpeed", ignore = true)
    WeatherDto toDto(Weather weather);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "windSpeed", ignore = true)
    Weather toEntity(WeatherDto dto);
}