package org.ikuzo.otboo.domain.weather.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoRegionDocument(
    String region_type,
    String address_name,
    String region_1depth_name,
    String region_2depth_name,
    String region_3depth_name,
    String region_4depth_name,
    String code,
    double x,
    double y
) {
}