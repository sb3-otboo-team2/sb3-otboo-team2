package org.ikuzo.otboo.domain.weather.client;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(WeatherApiProperties.class)
public class WeatherApiClient {

    private final WeatherApiProperties props;

    private volatile WebClient webClient;

    private WebClient client() {
        if (webClient == null) {
            synchronized (this) {
                if (webClient == null) {
                    ExchangeStrategies strategies = ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
                        .build();
                    webClient = WebClient.builder()
                        .baseUrl(props.getBaseUrl())
                        .exchangeStrategies(strategies)
                        .build();
                }
            }
        }
        return webClient;
    }

    /**
     * 예보 호출
     *
     * @param baseDate yyyyMMdd
     * @param baseTime HHmm (02:00,05:00,08:00,11:00,14:00,17:00,20:00,23:00 중 과거시각)
     * @param nx       기상청 격자 X
     * @param ny       기상청 격자 Y
     */
    public WeatherApiResponse getVillageforecast(String baseDate, String baseTime, int nx, int ny) {
        URI uri = UriComponentsBuilder.fromUriString(props.getBaseUrl())
            .path("/getVilageFcst")
            .queryParam("serviceKey", props.getServiceKey())
            .queryParam("pageNo", 1)
            .queryParam("numOfRows", 1000)
            .queryParam("dataType", "JSON")
            .queryParam("base_date", baseDate)
            .queryParam("base_time", baseTime)
            .queryParam("nx", nx)
            .queryParam("ny", ny)
            .build(true)
            .toUri();

        return client()
            .get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(WeatherApiResponse.class)
            .block(Duration.ofMillis(props.getTimeoutMs()));
    }

    /**
     * (Asia/Seoul) 기준 단기예보 baseDate/baseTime 계산
     */
    public Map<String, String> computeBaseDateTime(Instant nowSeoul) {
        LocalDateTime ldt = LocalDateTime.ofInstant(nowSeoul, ZoneId.of("Asia/Seoul"));
        // 유효 발표시각 세트
        int[] slots = {2, 5, 8, 11, 14, 17, 20, 23};
        int hour = ldt.getHour();
        int baseHour = 2;
        for (int s : slots) {
            if (hour >= s) {
                baseHour = s;
            }
        }
        // 00~01시는 전날 23시로
        LocalDate baseDate = ldt.toLocalDate();
        if (hour < 2) {
            baseDate = baseDate.minusDays(1);
            baseHour = 23;
        }
        String bd = baseDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String bt = String.format("%02d00", baseHour);
        return Map.of("baseDate", bd, "baseTime", bt);
    }
}