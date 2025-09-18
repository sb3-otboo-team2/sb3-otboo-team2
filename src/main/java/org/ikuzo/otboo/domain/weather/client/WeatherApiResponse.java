package org.ikuzo.otboo.domain.weather.client;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

// getVilageForecast 응답 간소화

public class WeatherApiResponse {
    private Response response;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Body {
        private Items items;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Items {
        private List<Item> item;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private String category;  // POP, PTY, PCP, REH, SKY, TMP, TMN, TMX, WSD ...
        private String baseDate;  // yyyyMMdd
        private String baseTime;  // HHmm (발표시각)
        private String fcstDate;  // yyyyMMdd
        private String fcstTime;  // HHmm
        private String fcstValue; // 예보값
        private int nx;           // X
        private int ny;           // Y
    }
}