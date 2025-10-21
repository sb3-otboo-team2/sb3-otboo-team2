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
public class PrecipitationDto {
    private PrecipitationType type; // NONE/RAIN/RAIN_SNOW/SNOW/SHOWER
    private Double amount;
    private Double probability;
}