package org.ikuzo.otboo.domain.weather.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.weather.client.KakaoLocalClient;
import org.ikuzo.otboo.domain.weather.client.WeatherApiClient;
import org.ikuzo.otboo.domain.weather.client.WeatherApiResponse;
import org.ikuzo.otboo.domain.weather.dto.HumidityDto;
import org.ikuzo.otboo.domain.weather.dto.KakaoRegionResponse;
import org.ikuzo.otboo.domain.weather.dto.PrecipitationDto;
import org.ikuzo.otboo.domain.weather.dto.PrecipitationType;
import org.ikuzo.otboo.domain.weather.dto.SkyStatus;
import org.ikuzo.otboo.domain.weather.dto.TemperatureDto;
import org.ikuzo.otboo.domain.weather.dto.WeatherAPILocation;
import org.ikuzo.otboo.domain.weather.dto.WeatherDto;
import org.ikuzo.otboo.domain.weather.dto.WindSpeedDto;
import org.ikuzo.otboo.domain.weather.dto.WindWord;
import org.ikuzo.otboo.domain.weather.exception.WeatherNoForecastException;
import org.ikuzo.otboo.domain.weather.util.KmaGridConverter;
import org.ikuzo.otboo.domain.weather.util.KmaGridConverter.XY;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherReadServiceImpl implements WeatherReadService {

    private final WeatherApiClient weatherApiClient;
    private final KakaoLocalClient kakaoLocalClient;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm");

    /**
     * GET /api/weathers/location
     */
    @Override
    @Transactional(readOnly = true)
    public WeatherAPILocation getLocation(double latitude, double longitude) {
        XY xy = KmaGridConverter.toXY(longitude, latitude);
        log.debug("[WeatherReadService] 좌표 변환 시작: lat={}, lon={} → grid=({}, {})", latitude, longitude, xy.x(), xy.y());

        KakaoRegionResponse kakao = kakaoLocalClient.coord2region(longitude, latitude);
        List<String> names = new ArrayList<>();
        if (kakao != null && kakao.documents() != null && !kakao.documents().isEmpty()) {
            var d = kakao.documents().get(0);
            if (notBlank(d.region_1depth_name())) {
                names.add(d.region_1depth_name());
            }
            if (notBlank(d.region_2depth_name())) {
                names.add(d.region_2depth_name());
            }
            if (notBlank(d.region_3depth_name())) {
                names.add(d.region_3depth_name());
            }
        }

        log.info("[WeatherReadService] 좌표({}, {}) → 행정구역명: {}", latitude, longitude, names);
        return WeatherAPILocation.builder()
            .latitude(latitude)
            .longitude(longitude)
            .x(xy.x()).y(xy.y())
            .locationNames(names)
            .build();
    }

    /**
     * GET /api/weathers
     */
    @Override
    @Transactional(readOnly = true)
    public List<WeatherDto> getWeatherByCoordinates(double latitude, double longitude) {
        log.debug("[WeatherReadService] 날씨 조회 시작: lat={}, lon={}", latitude, longitude);
        XY xy = KmaGridConverter.toXY(longitude, latitude);

        Map<String, String> base = weatherApiClient.computeBaseDateTime(Instant.now());
        String baseDate = base.get("baseDate");
        String baseTime = base.get("baseTime");

        WeatherApiResponse resp = weatherApiClient.getVillageforecast(baseDate, baseTime, xy.x(), xy.y());
        var items = Optional.ofNullable(resp)
            .map(WeatherApiResponse::getResponse)
            .map(WeatherApiResponse.Response::getBody)
            .map(WeatherApiResponse.Body::getItems)
            .map(WeatherApiResponse.Items::getItem)
            .orElse(List.of());

        if (items.isEmpty()) {
            log.warn("[WeatherReadService] 날씨 조회 실패: 기상청 예보 없음 (lat={}, lon={}, baseDate={}, baseTime={})",
                latitude, longitude, baseDate, baseTime);
            throw WeatherNoForecastException.withLatLonAndBase(latitude, longitude, baseDate, baseTime);
        }

        Map<String, Map<String, String>> byFcst = new TreeMap<>();
        for (var it : items) {
            String key = it.getFcstDate() + it.getFcstTime();
            byFcst.computeIfAbsent(key, k -> new HashMap<>()).put(it.getCategory(), it.getFcstValue());
        }

        WeatherAPILocation loc = getLocation(latitude, longitude);

        List<WeatherDto> result = new ArrayList<>();
        for (var entry : byFcst.entrySet()) {
            String fcstKey = entry.getKey();
            Map<String, String> cat = entry.getValue();

            Instant forecastedAt = toInstant(baseDate, baseTime);
            Instant forecastAt = toInstant(fcstKey.substring(0, 8), fcstKey.substring(8, 12));

            Double tmp = parseDouble(cat.get("TMP"));
            Double tmn = parseDouble(cat.get("TMN"));
            Double tmx = parseDouble(cat.get("TMX"));
            Double reh = parseDouble(cat.get("REH"));
            Double wsd = parseDouble(cat.get("WSD"));
            Double pop = parseDouble(cat.get("POP"));
            Double pcp = parsePrecipAmount(cat.get("PCP"));

            SkyStatus sky = mapSky(cat.get("SKY"));
            PrecipitationType pty = mapPty(cat.get("PTY"));
            WindWord windWord = toWindWord(wsd);

            result.add(WeatherDto.builder()
                .id(UUID.randomUUID())
                .forecastedAt(forecastedAt)
                .forecastAt(forecastAt)
                .location(loc)
                .skyStatus(sky)
                .precipitation(PrecipitationDto.builder()
                    .type(pty).amount(pcp).probability(pop).build())
                .humidity(HumidityDto.builder()
                    .current(reh).comparedToDayBefore(null).build())
                .temperature(TemperatureDto.builder()
                    .current(tmp).comparedToDayBefore(null).min(tmn).max(tmx).build())
                .windSpeed(WindSpeedDto.builder()
                    .speed(wsd).asWord(windWord).build())
                .build());
        }

        log.info("[WeatherReadService] 날씨 조회 완료: 좌표({}, {}), 예보 {}건", latitude, longitude, items.size());

        return result;
    }

    // ---------- helpers ----------
    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private Instant toInstant(String ymd, String hm) {
        LocalDate date = LocalDate.parse(ymd, DATE);
        LocalTime time = LocalTime.parse(hm, TIME);
        return ZonedDateTime.of(date, time, SEOUL).toInstant();
    }

    private Double parseDouble(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(v);
        } catch (Exception e) {
            return null;
        }
    }

    private Double parsePrecipAmount(String pcp) {
        if (pcp == null) {
            return null;
        }
        pcp = pcp.trim();
        if (pcp.equals("강수없음")) {
            return 0d;
        }
        String digits = pcp.replaceAll("[^0-9.]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(digits);
        } catch (Exception e) {
            return null;
        }
    }

    private SkyStatus mapSky(String sky) {
        if (sky == null) {
            return SkyStatus.MOSTLY_CLOUDY;
        }
        return switch (sky.trim()) {
            case "1" -> SkyStatus.CLEAR;
            case "3" -> SkyStatus.MOSTLY_CLOUDY;
            case "4" -> SkyStatus.CLOUDY;
            default -> SkyStatus.MOSTLY_CLOUDY;
        };
    }

    private PrecipitationType mapPty(String pty) {
        if (pty == null) {
            return PrecipitationType.NONE;
        }
        return switch (pty.trim()) {
            case "0" -> PrecipitationType.NONE;
            case "1" -> PrecipitationType.RAIN;
            case "2" -> PrecipitationType.RAIN_SNOW;
            case "3" -> PrecipitationType.SNOW;
            case "4" -> PrecipitationType.SHOWER;
            default -> PrecipitationType.NONE;
        };
    }

    private WindWord toWindWord(Double wsd) {
        if (wsd == null) {
            return null;
        }
        if (wsd < 4.0) {
            return WindWord.WEAK;
        }
        if (wsd < 10.0) {
            return WindWord.MODERATE;
        }
        return WindWord.STRONG;
    }
}