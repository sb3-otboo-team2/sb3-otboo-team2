package org.ikuzo.otboo.domain.weather.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.kma")
public class WeatherApiProperties {
    private String baseUrl;
    private String serviceKey;
    private int timeoutMs = 5000;
}