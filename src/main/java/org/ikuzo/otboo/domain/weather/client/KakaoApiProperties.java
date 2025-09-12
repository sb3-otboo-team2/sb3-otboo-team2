package org.ikuzo.otboo.domain.weather.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.kakao")
public class KakaoApiProperties {
    private String baseUrl;
    private String restApiKey;
}