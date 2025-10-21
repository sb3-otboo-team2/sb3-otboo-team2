package org.ikuzo.otboo.domain.weather.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
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
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.ikuzo.otboo.domain.weather.exception.WeatherNoForecastException;
import org.ikuzo.otboo.domain.weather.repository.WeatherRepository;
import org.ikuzo.otboo.domain.weather.util.KmaGridConverter;
import org.ikuzo.otboo.domain.weather.util.KmaGridConverter.XY;
import org.ikuzo.otboo.global.security.OtbooUserDetails;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherReadServiceImpl implements WeatherReadService {

    private final WeatherApiClient weatherApiClient;
    private final KakaoLocalClient kakaoLocalClient;
    private final WeatherRepository weatherRepository;
    private final UserRepository userRepository;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm");

    /**
     * GET /api/weathers/location
     */
    @Override
    @Transactional(readOnly = true)
    public WeatherAPILocation getLocation(double latitude, double longitude) {
        XY xy = KmaGridConverter.toXY(latitude, longitude);
        log.debug("[WeatherReadService] 좌표 변환 시작: lat={}, lon={} → grid=({}, {})", latitude, longitude, xy.x(), xy.y());

        KakaoRegionResponse kakao = kakaoLocalClient.coord2region(latitude, longitude);
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
    @Transactional
    @Cacheable(value = "weatherByCoordinates",
        key = "T(org.ikuzo.otboo.domain.weather.service.WeatherServiceImpl).buildCacheKey(#latitude, #longitude)")
    public List<WeatherDto> getWeatherByCoordinates(double latitude, double longitude) {
        log.debug("[WeatherReadService] 날씨 조회 시작: lat={}, lon={}", latitude, longitude);
        XY xy = KmaGridConverter.toXY(latitude, longitude);

        // 오늘 기준 예보 수집
        FetchResult today = fetchForecastItems(Instant.now(), xy.x(), xy.y());
        if (today.items().isEmpty()) {
            throw WeatherNoForecastException.withLatLonAndBase(latitude, longitude, today.baseDate(), today.baseTime());
        }

        // 어제 기준 예보 수집 (전일 비교용)
        FetchResult yesterday = fetchForecastItems(Instant.now().minus(Duration.ofDays(1)), xy.x(), xy.y());

        // 오늘 예보 아이템을 시각키(fcstDate+fcstTime)로 묶기
        Map<String, Map<String, String>> byFcst = groupByFcst(today.items());

        // 비교 인덱스 구성 (현재/전일 TMP, REH를 fcstKey로 조회할 수 있게)
        ComparisonIndex index = buildComparisonIndex(byFcst, yesterday.items());

        // 일 최저/최고 보정 값 계산 (TMN/TMX 우선, 없으면 TMP로 대체)
        DailyExtrema extrema = computeDailyExtrema(byFcst);

        // 위치 정보
        WeatherAPILocation loc = getLocation(latitude, longitude);

        // DTO 빌드
        List<WeatherDto> all = buildWeatherDtos(byFcst, today.baseDate(), today.baseTime(), loc, index, extrema);
        List<WeatherDto> filtered = filterTodayUpcomingLimit(all, 5);

        persistFetchedWeathers(filtered);

        // 오늘(Asia/Seoul) 이후 슬롯만 최대 5개
        log.info("[WeatherReadService] 날씨 조회 완료: 좌표({}, {})", latitude, longitude);

        return filtered;
    }


    private FetchResult fetchForecastItems(Instant baseInstant, int nx, int ny) {
        Map<String, String> base = weatherApiClient.computeBaseDateTime(baseInstant);
        String baseDate = base.get("baseDate");
        String baseTime = base.get("baseTime");

        WeatherApiResponse resp = weatherApiClient.getVillageforecast(baseDate, baseTime, nx, ny);
        List<WeatherApiResponse.Item> items = Optional.ofNullable(resp)
            .map(WeatherApiResponse::getResponse)
            .map(WeatherApiResponse.Response::getBody)
            .map(WeatherApiResponse.Body::getItems)
            .map(WeatherApiResponse.Items::getItem)
            .orElse(List.of());

        return new FetchResult(baseDate, baseTime, items);
    }

    private Map<String, Map<String, String>> groupByFcst(List<WeatherApiResponse.Item> items) {
        Map<String, Map<String, String>> byFcst = new TreeMap<>();
        for (WeatherApiResponse.Item it : items) {
            String key = it.getFcstDate() + it.getFcstTime();
            byFcst.computeIfAbsent(key, k -> new HashMap<>()).put(it.getCategory(), it.getFcstValue());
        }
        return byFcst;
    }

    private ComparisonIndex buildComparisonIndex(Map<String, Map<String, String>> byFcst,
                                                 List<WeatherApiResponse.Item> itemsPrev) {
        Map<String, Double> tmpByKey = new HashMap<>();
        Map<String, Double> rehByKey = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> e : byFcst.entrySet()) {
            String key = e.getKey();
            Map<String, String> cat = e.getValue();
            Double tmp = parseDouble(cat.get("TMP"));
            Double reh = parseDouble(cat.get("REH"));
            if (tmp != null) {
                tmpByKey.put(key, tmp);
            }
            if (reh != null) {
                rehByKey.put(key, reh);
            }
        }

        Map<String, Double> tmpPrevByKey = new HashMap<>();
        Map<String, Double> rehPrevByKey = new HashMap<>();
        for (WeatherApiResponse.Item it : itemsPrev) {
            String key = it.getFcstDate() + it.getFcstTime();
            String cat = it.getCategory();
            if ("TMP".equals(cat)) {
                tmpPrevByKey.put(key, parseDouble(it.getFcstValue()));
            } else if ("REH".equals(cat)) {
                rehPrevByKey.put(key, parseDouble(it.getFcstValue()));
            }
        }
        return new ComparisonIndex(tmpByKey, rehByKey, tmpPrevByKey, rehPrevByKey);
    }

    private DailyExtrema computeDailyExtrema(Map<String, Map<String, String>> byFcst) {
        Map<String, Double> dailyMinTmp = new HashMap<>();
        Map<String, Double> dailyMaxTmp = new HashMap<>();
        Map<String, Double> dailyTMN = new HashMap<>();
        Map<String, Double> dailyTMX = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> e : byFcst.entrySet()) {
            String key = e.getKey();
            String ymd = key.substring(0, 8);
            Map<String, String> cat = e.getValue();

            Double tmp = parseDouble(cat.get("TMP"));
            Double tmn = parseDouble(cat.get("TMN"));
            Double tmx = parseDouble(cat.get("TMX"));

            if (tmp != null) {
                dailyMinTmp.merge(ymd, tmp, Math::min);
                dailyMaxTmp.merge(ymd, tmp, Math::max);
            }
            if (tmn != null) {
                dailyTMN.merge(ymd, tmn, Math::min);
            }
            if (tmx != null) {
                dailyTMX.merge(ymd, tmx, Math::max);
            }
        }

        Map<String, Double> minByDay = new HashMap<>();
        Map<String, Double> maxByDay = new HashMap<>();
        for (String ymd : dailyMinTmp.keySet()) {
            minByDay.put(ymd, dailyTMN.getOrDefault(ymd, dailyMinTmp.get(ymd)));
            maxByDay.put(ymd, dailyTMX.getOrDefault(ymd, dailyMaxTmp.get(ymd)));
        }
        return new DailyExtrema(minByDay, maxByDay);
    }

    private List<WeatherDto> buildWeatherDtos(Map<String, Map<String, String>> byFcst,
                                              String baseDate,
                                              String baseTime,
                                              WeatherAPILocation loc,
                                              ComparisonIndex index,
                                              DailyExtrema extrema) {
        List<WeatherDto> result = new ArrayList<>();

        for (Map.Entry<String, Map<String, String>> entry : byFcst.entrySet()) {
            String fcstKey = entry.getKey();
            Map<String, String> cat = entry.getValue();

            Instant forecastedAt = toInstant(baseDate, baseTime);
            Instant forecastAt = toInstant(fcstKey.substring(0, 8), fcstKey.substring(8, 12));

            String ymd = fcstKey.substring(0, 8);
            String hm = fcstKey.substring(8, 12);

            String prevKey = LocalDate.parse(ymd, DATE).minusDays(1).format(DATE) + hm;
            String prev3hKey = minusHoursKey(fcstKey, 3);

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

            // 비교값: 전일 같은 시각 → 없으면 직전 3시간
            Double tempPrev = firstNonNull(index.tmpPrevByKey().get(prevKey), index.tmpByKey().get(prev3hKey));
            Double rehPrev = firstNonNull(index.rehPrevByKey().get(prevKey), index.rehByKey().get(prev3hKey));

            Double tempCompared = (tmp != null && tempPrev != null) ? round1(tmp - tempPrev) : null;
            Double humidCompared = (reh != null && rehPrev != null) ? round1(reh - rehPrev) : null;

            Double minForDay = (tmn != null) ? tmn : extrema.minByDay().get(ymd);
            Double maxForDay = (tmx != null) ? tmx : extrema.maxByDay().get(ymd);
            Double probability01 = (pop != null) ? pop / 100.0 : null;

            result.add(WeatherDto.builder()
                .forecastedAt(forecastedAt)
                .forecastAt(forecastAt)
                .location(loc)
                .skyStatus(sky)
                .precipitation(PrecipitationDto.builder()
                    .type(pty).amount(pcp).probability(probability01).build())
                .humidity(HumidityDto.builder()
                    .current(reh).comparedToDayBefore(humidCompared).build())
                .temperature(TemperatureDto.builder()
                    .current(tmp).comparedToDayBefore(tempCompared).min(minForDay).max(maxForDay).build())
                .windSpeed(WindSpeedDto.builder()
                    .speed(wsd).asWord(windWord).build())
                .build());
        }

        return result;
    }

    private List<WeatherDto> filterTodayUpcomingLimit(List<WeatherDto> all, int limit) {
        all.sort(Comparator.comparing(WeatherDto::getForecastAt));
        Instant now = Instant.now();

        Map<LocalDate, WeatherDto> perDay = new LinkedHashMap<>();
        for (WeatherDto dto : all) {
            if (dto.getForecastAt().isBefore(now)) {
                continue;
            }
            LocalDate date = dto.getForecastAt().atZone(SEOUL).toLocalDate();
            perDay.putIfAbsent(date, dto);
            if (perDay.size() >= limit) {
                break;
            }
        }

        return perDay.values().stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    private void persistFetchedWeathers(List<WeatherDto> forecasts) {
        Optional<User> userOpt = currentUser();
        if (userOpt.isEmpty()) {
            log.debug("[WeatherReadService] 인증된 사용자 없음 → 예보 저장 생략");
            forecasts.forEach(dto -> dto.setId(null));
            return;
        }

        User user = userOpt.get();
        forecasts.forEach(dto -> saveForecast(user, dto));
    }

    private void saveForecast(User user, WeatherDto dto) {
        if (!isPersistable(dto)) {
            log.debug("[WeatherReadService] 필수 데이터 누락으로 저장 생략: forecastAt={}", dto.getForecastAt());
            dto.setId(null);
            return;
        }

        Weather candidate = convertToEntity(user, dto);
        Weather entityToPersist = weatherRepository.findByUserAndForecastAt(user, candidate.getForecastAt())
            .map(existing -> {
                existing.updateFrom(candidate);
                return existing;
            })
            .orElse(candidate);

        Weather saved = weatherRepository.save(entityToPersist);
        dto.setId(saved.getId());
    }

    private Weather convertToEntity(User user, WeatherDto dto) {
        PrecipitationDto precipitation = dto.getPrecipitation();
        TemperatureDto temperature = dto.getTemperature();
        HumidityDto humidity = dto.getHumidity();
        WindSpeedDto wind = dto.getWindSpeed();

        return Weather.builder()
            .user(user)
            .forecastedAt(dto.getForecastedAt())
            .forecastAt(dto.getForecastAt())
            .skyStatus(dto.getSkyStatus().name())
            .precipitationType(selectPrecipitationType(precipitation))
            .precipitationAmount(precipitation != null ? precipitation.getAmount() : null)
            .precipitationProbability(
                probabilityToPercent(precipitation != null ? precipitation.getProbability() : null))
            .temperatureCurrent(temperature.getCurrent())
            .temperatureCompared(temperature.getComparedToDayBefore())
            .temperatureMin(temperature.getMin())
            .temperatureMax(temperature.getMax())
            .windSpeed(wind != null ? wind.getSpeed() : null)
            .windSpeedWord(wind != null && wind.getAsWord() != null ? wind.getAsWord().name() : null)
            .humidityCurrent(humidity != null ? humidity.getCurrent() : null)
            .humidityCompared(humidity != null ? humidity.getComparedToDayBefore() : null)
            .build();
    }

    private boolean isPersistable(WeatherDto dto) {
        if (dto == null || dto.getSkyStatus() == null || dto.getForecastAt() == null || dto.getForecastedAt() == null) {
            return false;
        }
        TemperatureDto temperature = dto.getTemperature();
        PrecipitationDto precipitation = dto.getPrecipitation();
        return temperature != null && temperature.getCurrent() != null
            && precipitation != null && precipitation.getProbability() != null;
    }

    private String selectPrecipitationType(PrecipitationDto precipitation) {
        if (precipitation == null || precipitation.getType() == null) {
            return PrecipitationType.NONE.name();
        }
        return precipitation.getType().name();
    }

    private Double probabilityToPercent(Double probability) {
        return probability == null ? 0d : probability * 100.0;
    }

    private Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String principalName && "anonymousUser".equals(principalName)) {
            return Optional.empty();
        }
        if (!(principal instanceof OtbooUserDetails details)) {
            return Optional.empty();
        }
        return userRepository.findById(details.getUserDto().id());
    }

    private record FetchResult(String baseDate, String baseTime, List<WeatherApiResponse.Item> items) {
    }

    private record ComparisonIndex(Map<String, Double> tmpByKey,
                                   Map<String, Double> rehByKey,
                                   Map<String, Double> tmpPrevByKey,
                                   Map<String, Double> rehPrevByKey) {
    }

    private record DailyExtrema(Map<String, Double> minByDay, Map<String, Double> maxByDay) {
    }

    private String minusHoursKey(String ymdHm, int hours) {
        LocalDate date = LocalDate.parse(ymdHm.substring(0, 8), DATE);
        LocalTime time = LocalTime.parse(ymdHm.substring(8, 12), TIME);
        ZonedDateTime zdt = ZonedDateTime.of(date, time, SEOUL).minusHours(hours);
        return zdt.format(DATE) + zdt.format(TIME);
    }

    private <T> T firstNonNull(T a, T b) {
        return (a != null) ? a : b;
    }

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

    private Double round1(Double v) {
        return (v == null) ? null : Math.round(v * 10d) / 10d;
    }
}
