package org.ikuzo.otboo.domain.weather.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoRegionResponse(
    KakaoRegionMeta meta,
    List<KakaoRegionDocument> documents
) {
}