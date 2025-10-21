package org.ikuzo.otboo.domain.weather.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.exception.UserLocationMissingException;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.domain.weather.client.KakaoLocalClient;
import org.ikuzo.otboo.domain.weather.client.WeatherApiClient;
import org.ikuzo.otboo.domain.weather.client.WeatherApiResponse;
import org.ikuzo.otboo.domain.weather.client.WeatherApiResponse.Body;
import org.ikuzo.otboo.domain.weather.client.WeatherApiResponse.Item;
import org.ikuzo.otboo.domain.weather.client.WeatherApiResponse.Items;
import org.ikuzo.otboo.domain.weather.client.WeatherApiResponse.Response;
import org.ikuzo.otboo.domain.weather.dto.KakaoRegionDocument;
import org.ikuzo.otboo.domain.weather.dto.KakaoRegionResponse;
import org.ikuzo.otboo.domain.weather.dto.RegionInfoDto;
import org.ikuzo.otboo.domain.weather.dto.WeatherDto;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.ikuzo.otboo.domain.weather.exception.WeatherNoForecastException;
import org.ikuzo.otboo.domain.weather.mapper.WeatherMapper;
import org.ikuzo.otboo.domain.weather.repository.WeatherRepository;
import org.ikuzo.otboo.domain.weather.util.KmaGridConverter;
import org.ikuzo.otboo.domain.weather.util.KmaGridConverter.XY;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    private final WeatherApiClient weatherApiClient;
    private final KakaoLocalClient kakaoLocalClient;
    private final WeatherRepository weatherRepository;
    private final WeatherMapper weatherMapper;
    private final UserRepository userRepository;
    private final WeatherAlertServiceImpl weatherAlertServiceImpl;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm");

    public static String buildCacheKey(double latitude, double longitude) {
        return String.format("%.4f:%.4f", latitude, longitude);
    }

    // 위경도 -> 행정구역 명
    @Transactional(readOnly = true)
    @Override
    public RegionInfoDto reverseGeocode(double latitude, double longitude) {
        KakaoRegionResponse res = kakaoLocalClient.coord2region(latitude, longitude);
        if (res.documents() == null || res.documents().isEmpty()) {
            return RegionInfoDto.builder().addressName("UNKNOWN").x(longitude).y(latitude).build();
        }
        KakaoRegionDocument d = res.documents().get(0);
        return RegionInfoDto.builder()
            .addressName(d.address_name())
            .region1DepthName(d.region_1depth_name())
            .region2DepthName(d.region_2depth_name())
            .region3DepthName(d.region_3depth_name())
            .code(d.code())
            .x(d.x())
            .y(d.y())
            .build();
    }

    //단기예보 수집 -> Weather 저장 -> 변화 감지 후 알림
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public WeatherDto collectAndSaveForUser(UUID userId) {
        log.debug("[WeatherService] 사용자 {} 날씨 수집 시작", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.: " + userId)); //유저 예외처리 대기

        if (user.getLatitude() == null || user.getLongitude() == null) {
            log.warn("[WeatherService] 사용자 {}: 위도/경도 정보 없음 → 수집 불가", userId);
            throw UserLocationMissingException.withUserId(userId);
        }

        XY xy = KmaGridConverter.toXY(user.getLatitude(), user.getLongitude());

        // 기상청 baseDate/baseTime 계산
        Instant now = Instant.now();
        Map<String, String> base = weatherApiClient.computeBaseDateTime(now);
        String baseDate = base.get("baseDate");
        String baseTime = base.get("baseTime");

        log.debug("[WeatherService] 사용자 {}: KMA API 호출 baseDate={}, baseTime={}, grid=({}, {})",
            userId, baseDate, baseTime, xy.x(), xy.y());

        WeatherApiResponse resp = weatherApiClient.getVillageforecast(baseDate, baseTime, xy.x(), xy.y());
        List<Item> items = Optional.ofNullable(resp)
            .map(WeatherApiResponse::getResponse)
            .map(Response::getBody)
            .map(Body::getItems)
            .map(Items::getItem)
            .orElse(List.of());

        // 동일 fcstDate+fcstTime 기준 예보 묶음 만들기
        Map<String, Map<String, String>> grouped = new LinkedHashMap<>();
        for (Item it : items) {
            String key = it.getFcstDate() + it.getFcstTime();
            grouped.computeIfAbsent(key, k -> new HashMap<>()).put(it.getCategory(), it.getFcstValue());
        }

        if (grouped.isEmpty()) {
            log.warn("[WeatherService] 사용자 {}: 기상청 응답 없음 (baseDate={}, baseTime={})", userId, baseDate, baseTime);
            throw WeatherNoForecastException.withBaseAndGrid(baseDate, baseTime, xy.x(), xy.y());
        }

        log.debug("[WeatherService] 사용자 {}: 기상청 예보 {}건 수신", userId, items.size());

        // 첫 번째(가장 이른 키) 선택
        String firstKey = grouped.keySet().stream().min(String::compareTo).orElseThrow();
        Map<String, String> cat = grouped.get(firstKey);

        if (!cat.containsKey("TMP") || !cat.containsKey("POP") || !cat.containsKey("SKY") || !cat.containsKey("PTY")) {
            log.warn("[WeatherService] 사용자 {}: 필수 카테고리 누락 (keys={})", userId, cat.keySet());
            throw WeatherNoForecastException.withBaseAndGrid(baseDate, baseTime, xy.x(), xy.y());
        }

        // 매핑
        Weather w = mapForecastToEntity(user, baseDate, baseTime, firstKey.substring(0, 8), firstKey.substring(8, 12),
            cat);
        Weather saved = weatherRepository.save(w);

        log.info("[WeatherService] 사용자 {} 날씨 저장 완료: forecastAt={}", userId, saved.getForecastAt());

        // 이전값 대비 변화 감지 -> 알림
        weatherAlertServiceImpl.checkAndNotify(user, saved);

        return weatherMapper.toDto(saved);
    }

    // 카테고리 매핑 로직

    private Weather mapForecastToEntity(User user,
                                        String baseDate, String baseTime,
                                        String fcstDate, String fcstTime,
                                        Map<String, String> cat) {
        Instant forecastedAt = toInstant(baseDate, baseTime);
        Instant forecastAt = toInstant(fcstDate, fcstTime);

        Double tmp = parseDouble(cat.get("TMP")); // 현재 기온
        Double reh = parseDouble(cat.get("REH")); // 습도 %
        Double pop = parseDouble(cat.get("POP")); // 강수확률 %
        Double wsd = parseDouble(cat.get("WSD")); // 풍속 m/s

        Double tmn = parseDouble(cat.get("TMN"));
        Double tmx = parseDouble(cat.get("TMX"));

        Double pcp = parsePrecipAmount(cat.get("PCP")); // "강수없음" or "1.0mm"

        String sky = mapSky(cat.get("SKY")); // 1,3,4 → CLEAR/MOSTLY_CLOUDY/CLOUDY
        String pty = mapPty(cat.get("PTY")); // 0,1,2,3,4 → NONE/RAIN/RAIN_SNOW/SNOW/SHOWER
        String windWord = toWindWord(wsd);

        // 이전 날씨 대비 비교값 계산
        Optional<Weather> prevOpt = weatherRepository.findTop1ByUserOrderByForecastAtDesc(user);
        Double tempCompared = prevOpt.map(p -> diff(tmp, p.getTemperatureCurrent())).orElse(null);
        Double humidCompared = prevOpt.map(p -> diff(reh, p.getHumidityCurrent())).orElse(null);

        return Weather.builder()
            .user(user)
            .forecastedAt(forecastedAt)
            .forecastAt(forecastAt)
            .skyStatus(sky)
            .precipitationType(pty)
            .precipitationAmount(pcp)
            .precipitationProbability(pop)
            .temperatureCurrent(tmp)
            .temperatureCompared(tempCompared)
            .temperatureMin(tmn)
            .temperatureMax(tmx)
            .windSpeed(wsd)
            .windSpeedWord(windWord)
            .humidityCurrent(reh)
            .humidityCompared(humidCompared)
            .build();
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

    private String mapSky(String sky) {
        if (sky == null) {
            return "MOSTLY_CLOUDY";
        }
        switch (sky.trim()) {
            case "1":
                return "CLEAR";
            case "3":
                return "MOSTLY_CLOUDY";
            case "4":
                return "CLOUDY";
            default:
                return "MOSTLY_CLOUDY";
        }
    }

    private String mapPty(String pty) {
        if (pty == null) {
            return "NONE";
        }
        switch (pty.trim()) {
            case "0":
                return "NONE";
            case "1":
                return "RAIN";
            case "2":
                return "RAIN_SNOW";
            case "3":
                return "SNOW";
            case "4":
                return "SHOWER";
            default:
                return "NONE";
        }
    }

    private String toWindWord(Double wsd) {
        if (wsd == null) {
            return null;
        }
        if (wsd < 4.0) {
            return "WEAK";
        }
        if (wsd < 10.0) {
            return "MODERATE";
        }
        return "STRONG";
    }

    private Double diff(Double cur, Double prev) {
        if (cur == null || prev == null) {
            return null;
        }
        return Math.round((cur - prev) * 10d) / 10d;
    }
}