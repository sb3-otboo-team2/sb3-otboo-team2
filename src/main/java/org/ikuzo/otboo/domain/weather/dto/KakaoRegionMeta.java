package org.ikuzo.otboo.domain.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoRegionMeta(
    int total_count
) {
}