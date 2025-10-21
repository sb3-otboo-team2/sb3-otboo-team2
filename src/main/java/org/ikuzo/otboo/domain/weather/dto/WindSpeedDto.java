package org.ikuzo.otboo.domain.weather.dto;

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
public class WindSpeedDto {
    private Double speed;
    private WindWord asWord; // WEAK/MODERATE/STRONG
}