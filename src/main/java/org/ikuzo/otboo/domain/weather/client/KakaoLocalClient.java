package org.ikuzo.otboo.domain.weather.client;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.weather.dto.KakaoRegionResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(KakaoApiProperties.class)
public class KakaoLocalClient {

    private final KakaoApiProperties props;

    private WebClient client() {
        String base = props.getBaseUrl();
        if (base == null || base.isBlank()) {
            base = "https://dapi.kakao.com";
        }
        return WebClient.builder()
            .baseUrl(base)
            .defaultHeader("Authorization", "KakaoAK " + props.getRestApiKey())
            .build();
    }

    public KakaoRegionResponse cord2region(double lon, double lat) {
        return client()
            .get()
            .uri(uriBuilder -> uriBuilder.path("/v2/local/geo/coord2regioncode.json")
                .queryParam("x", lon)
                .queryParam("y", lat)
                .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(KakaoRegionResponse.class)
            .block(Duration.ofSeconds(5));
    }
}