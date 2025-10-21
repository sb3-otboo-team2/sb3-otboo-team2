package org.ikuzo.otboo.domain.weather.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(name = "WeatherAPILocation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherAPILocation {
    private double latitude;
    private double longitude;
    private int x;
    private int y;
    private List<String> locationNames; // ["서울특별시","중구","태평로1가"]
}