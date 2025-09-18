package org.ikuzo.otboo.domain.weather.client;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.kakao")
public class KakaoApiProperties {
    @URL
    @NotBlank
    private String baseUrl;
    @NotBlank
    private String restApiKey;
}