package org.ikuzo.otboo.domain.weather.dto;

import lombok.Builder;

@Builder
public record RegionInfoDto(
    String addressName,       // 전체 지역 명칭
    String region1DepthName,  // 시/도
    String region2DepthName,  // 시/군/구
    String region3DepthName,  // 읍/면/동
    String code,              // 행정코드
    Double x,                 // 경도
    Double y                  // 위도
) {
}