package org.ikuzo.otboo.domain.recommendation.service.engine.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttribute;
import org.ikuzo.otboo.domain.clothes.enums.ClothesType;
import org.ikuzo.otboo.domain.clothes.repository.ClothesRepository;
import org.ikuzo.otboo.domain.recommendation.service.engine.RecommendationEngine;
import org.ikuzo.otboo.domain.recommendation.temp.KmaPerceivedTemperature;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleBasedRecommendationEngine implements RecommendationEngine {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ClothesRepository clothesRepository;

    private double lastPtDay;
    private double lastPtNight;

    // 아우터 추천 기준
    private static final double OUTER_NEED_NIGHT_COOL = 20.0; // ptNight ≤ 20 → 아우터 필요
    private static final double OUTER_NEED_CURRENT_HOT = 23.0; // 현재 온도를 기반으로 아우터 판단

    // 과도기 온도 기준
    private static final double MARCH_COLD_PT = 12.0;  // 이하면 겨울로 보는 쪽
    private static final double SEPT_HOT_PT   = 25.0;  // 이상이면 여름으로 보는 쪽

    // 계절 약가점
    private static final int SEASON_PREF_EXACT = 3;  // 현재 계절 정확 일치
    private static final int SEASON_PREF_ALL = 1;  // 사계절
    private static final int SEASON_PREF_PAIR = 2;  // 봄↔가을 교차

    // 타입별 추천 최소 총점
    private static final int FLOOR_PRIMARY          = 2; // TOP/DRESS
    private static final int FLOOR_BOTTOM           = 1; // BOTTOM
    private static final int FLOOR_OUTER_REQUIRED   = 1; // OUTER(필수 모드에서조차 이 미만이면 생략)
    private static final int FLOOR_MISC             = 0; // SHOES/HAT/SCARF

    // ===== 스타일 점수(맵 기반) =====
    private static final int STYLE_EXACT = 5; // 동일 스타일
    private static final int STYLE_COMPAT = 3; // 호환/공용
    private static final int STYLE_DEFAULT = 0; // 그 외
    private static final String STYLE_ANY = "기본";

    // ===== 재질 감점(강수 확률 ≥50%) =====
    private static final int PENALTY_RAIN_SUEDE = 3; // 비+스웨이드
    private static final int PENALTY_RAIN_LEATHER = 2; // 비+가죽/레더
    private static final int PENALTY_SNOW_SUEDE = 3; // 눈+스웨이드
    private static final int PENALTY_SNOW_LEATHER = 2; // 눈+가죽/레더

    /**
     * 스타일 호환표(앵커→호환집합). 필요 시 항목만 튜닝
     */
    private static final Map<String, Set<String>> COMPAT = Map.of(
        "캐주얼", Set.of("캐주얼", "빈티지", "러블리"),
        "미니멀", Set.of("미니멀", "포멀", "시크"),
        "포멀", Set.of("포멀", "미니멀", "시크"),
        "시크", Set.of("시크", "미니멀", "포멀"),
        "빈티지", Set.of("빈티지", "캐주얼"),
        "러블리", Set.of("러블리", "캐주얼")
    );


    @Override
    public List<Clothes> recommend(User owner, Weather weather) {

        final double ta = nz(weather.getTemperatureCurrent(), 20.0);
        final double rh = nz(weather.getHumidityCurrent(), 50.0);
        final double wind = windMs(weather);
        final double minC = nz(weather.getTemperatureMin(), ta);
        final double personalSensitivity = nz(Double.valueOf(owner.getTemperatureSensitivity()), 3.0);

        final Instant forecastUtc = firstNonNull(
            toInstantSafe(weather.getForecastAt()),
            toInstantSafe(weather.getCreatedAt()),
            Instant.now()
        );
        double kmaTmpDay = KmaPerceivedTemperature.compute(ta, rh, wind, forecastUtc);
        double kmaTmpNight = KmaPerceivedTemperature.compute(minC, rh, wind, forecastUtc);
        final double ptDay = calculatePersonalTemperature(kmaTmpDay, personalSensitivity);
        final double ptNight =  calculatePersonalTemperature(kmaTmpNight, personalSensitivity);
        this.lastPtDay = ptDay;
        this.lastPtNight = ptNight;

        final String seasonNow = seasonByMonthWithTransition(forecastUtc, ptDay); // "봄/여름/가을/겨울"
        final String precipitation = enumName(weather.getPrecipitationType()); // "RAIN"/"SNOW"/"NONE"...
        final Integer precipitationProb = toInt(weather.getPrecipitationProbability()); // null 가능

        final boolean outerNeeded = isOuterNeeded(ptDay, ptNight);

        log.info("ptDay: {}, ptNight: {}, seasonNow: {}",ptDay,ptNight,seasonNow);

        List<Clothes> candidateClothes = clothesRepository.findByOwnerId(owner.getId());
        if (candidateClothes.isEmpty()) {
            return List.of();
        }

        Map<ClothesType, List<Clothes>> byType = candidateClothes.stream()
            .collect(Collectors.groupingBy(Clothes::getType));

        if (outerNeeded) {
            log.info("아우터 점수 계산 실시");
            Clothes outer = pickBestWithFloor(byType.get(ClothesType.OUTER), seasonNow, null, precipitation, precipitationProb, FLOOR_OUTER_REQUIRED);

            if (outer == null) {
                log.info("아우터는 필요하지만 OUTER 후보가 기준점 미달 혹은 없음 → OUTER 없이 진행");
                return pickWithoutOuter(byType, seasonNow, precipitation, precipitationProb);
            }

            List<Clothes> result = new ArrayList<>();
            result.add(outer);
            log.info("상의 및 드레스 점수 계산 실시");
            Clothes top = pickBestWithFloor(byType.get(ClothesType.TOP), seasonNow, outer, precipitation, precipitationProb,FLOOR_PRIMARY);
            Clothes dress = pickBestWithFloor(byType.get(ClothesType.DRESS), seasonNow, outer, precipitation, precipitationProb,FLOOR_PRIMARY);
            Clothes inner = betterOf(top, dress, seasonNow, outer, precipitation, precipitationProb);

            if (inner != null) {
                result.add(inner);
                if (inner.getType() == ClothesType.TOP) {
                    log.info("하의 점수 계산 실시");
                    Clothes bottom = pickBestWithFloor(byType.get(ClothesType.BOTTOM), seasonNow, outer, precipitation, precipitationProb, FLOOR_BOTTOM);
                    if (bottom != null) {
                        result.add(bottom);
                    }
                }
            } else {
                log.info("상의 또는 드레스가 존재하지 않음(혹은 기준점 미달) → 하의만이라도 시도");
                log.info("하의 점수 계산 실시");
                Clothes bottom = pickBestWithFloor(byType.get(ClothesType.BOTTOM), seasonNow, outer, precipitation, precipitationProb, FLOOR_BOTTOM);
                if (bottom != null) {
                    result.add(bottom);
                }
            }

            log.info("신발 점수 계산 실시");
            addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.SHOES), seasonNow, outer, precipitation, precipitationProb, FLOOR_MISC));
            log.info("모자 점수 계산 실시");
            addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.HAT),   seasonNow, outer, precipitation, precipitationProb, FLOOR_MISC));
            log.info("스카프 점수 계산 실시");
            addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.SCARF), seasonNow, outer, precipitation, precipitationProb, FLOOR_MISC));
            log.info("악세서리 점수 계산 실시");
            addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.ACCESSORY), seasonNow, outer, precipitation, precipitationProb, FLOOR_MISC));

            return result;
        }

        log.info("아우터 불필요");
        return pickWithoutOuter(byType, seasonNow, precipitation, precipitationProb);

    }

    // ────────────────────────────────────────────────────────────────────────
    // 내부 선택 로직
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 아우터 필요: (1) 밤 체감 ≤ 20°C, (2) 낮 체감 ≤ 23°C
     */
    private boolean isOuterNeeded(double ptDay, double ptNight) {
        if (ptNight <= OUTER_NEED_NIGHT_COOL || ptDay <= OUTER_NEED_CURRENT_HOT) {
            return true;
        }
        return false;
    }

    private List<Clothes> pickWithoutOuter(Map<ClothesType, List<Clothes>> byType, String seasonNow, String precipitation, Integer precipitationProb
    ) {
        List<Clothes> result = new ArrayList<>();

        log.info("상의 및 드레스 점수 계산 실시");
        Clothes topCandidate   = pickBestWithFloor(byType.get(ClothesType.TOP),   seasonNow, null, precipitation, precipitationProb, FLOOR_PRIMARY);
        Clothes dressCandidate = pickBestWithFloor(byType.get(ClothesType.DRESS), seasonNow, null, precipitation, precipitationProb, FLOOR_PRIMARY);

        log.info("모든 상의 후보 수: {}", byType.getOrDefault(ClothesType.TOP, List.of()).size());
        log.info("상의 - {}, 드레스 - {}", topCandidate, dressCandidate);

        Clothes primary = betterOf(topCandidate, dressCandidate, seasonNow, null, precipitation, precipitationProb);
        log.info("아우터가 필요 없는 상황에서 골라진 메인 - {}", primary);

        if (primary != null) {
            result.add(primary);
            if (primary.getType() == ClothesType.TOP) {
                log.info("하의 점수 계산 실시");
                Clothes bottom = pickBestWithFloor(byType.get(ClothesType.BOTTOM), seasonNow, primary, precipitation, precipitationProb, FLOOR_BOTTOM);
                if (bottom != null) {
                    result.add(bottom);
                }
            }
        } else {
            log.info("하의 점수 계산 실시");
            Clothes bottom = pickBestWithFloor(byType.get(ClothesType.BOTTOM), seasonNow, null, precipitation, precipitationProb, FLOOR_BOTTOM);
            if (bottom != null) {
                result.add(bottom);
            }
        }

        log.info("신발 점수 계산 실시");
        addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.SHOES), seasonNow, primary, precipitation, precipitationProb, FLOOR_MISC));
        log.info("모자 점수 계산 실시");
        addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.HAT),   seasonNow, primary, precipitation, precipitationProb, FLOOR_MISC));
        log.info("스카프 점수 계산 실시");
        addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.SCARF), seasonNow, primary, precipitation, precipitationProb, FLOOR_MISC));
        log.info("악세서리 점수 계산 실시");
        addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.ACCESSORY), seasonNow, primary, precipitation, precipitationProb, FLOOR_MISC));

        return result;
    }

    private Clothes pickBestWithFloor(List<Clothes> list, String seasonNow, Clothes anchor, String precipitation,
        Integer precipitationProb, int floorTotal) {
        if (list == null || list.isEmpty()) return null;
        String anchorStyle = (anchor == null) ? null : attr(anchor, "스타일");
        int best = Integer.MIN_VALUE;
        Clothes bestItem = null;
        for (Clothes c : list) {
            int s = totalScore(c, seasonNow, anchorStyle, precipitation, precipitationProb, anchor);
            if (s > best) { best = s; bestItem = c; }
        }
        // 최소 기준점 미만이면 추천하지 않음
        if (bestItem != null && best >= floorTotal) return bestItem;
        return null;
    }

    /**
     * 총점 = (계절 점수) + (스타일 점수) + (재질×강수 감점) + (두께 점수)
     */
    private int totalScore(Clothes c, String seasonNow, String anchorStyle,
        String precipitation, Integer precipitationProb, Clothes anchor) {
        String itemSeason = attr(c, "계절");
        String itemStyle = attr(c, "스타일");

        int sSeason = seasonAffinityScore(seasonNow, itemSeason);
        int sStyle = styleScore(anchorStyle, itemStyle);
        int sPenalty = materialPenalty(c, precipitation, precipitationProb);
        int sThick = thicknessScore(c, anchor);

        int total = sSeason + sStyle + sPenalty + sThick;
        log.info("의상: {}, 점수 - Season:{}, Style:{}, Penalty:{}, Thick:{}, Total:{}",
            c.getName(), sSeason, sStyle, sPenalty, sThick, total);
        return total;
    }

    /**
     * TOP vs DRESS 중 더 점수가 높은 것을 선택 (둘 다 null이면 null)
     */
    private Clothes betterOf(Clothes a, Clothes b, String seasonNow, Clothes anchor,
        String precipitation, Integer precipitationProb) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        String anchorStyle = (anchor == null) ? null : attr(anchor, "스타일");
        int sa = totalScore(a, seasonNow, anchorStyle, precipitation, precipitationProb, anchor);
        int sb = totalScore(b, seasonNow, anchorStyle, precipitation, precipitationProb, anchor);
        return (sa >= sb) ? a : b;
    }

    /** 계절 가점(정확 +2, 봄↔가을 +1, 사계절 +1, 그 외 0) ★ 변경 */
    private int seasonAffinityScore(String now, String item) {
        if (item == null || item.isBlank()) return 0;
        if ("사계절".equals(item)) return SEASON_PREF_ALL;
        if (item.equals(now)) return SEASON_PREF_EXACT;
        boolean weakPair =
            ("봄".equals(now)   && ("가을".equals(item) || "겨울".equals(item))) ||
                ("가을".equals(now) && ("봄".equals(item)  || "여름".equals(item))) ||
                ("여름".equals(now) &&  "가을".equals(item)) ||
                ("겨울".equals(now) &&  "봄".equals(item));
        return weakPair ? SEASON_PREF_PAIR : 0;
    }

    // ────────────────────────────────────────────────────────────────────────
    // 재질 × 강수 확률 감점
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 강수 확률이 50% 이상일 때만 재질 감점을 적용한다. (배제가 아닌 후순위화)
     */
    private int materialPenalty(Clothes c, String precipitation, Integer prob) {
        if (precipitation == null || prob == null || prob < 50) {
            return 0;
        }

        String m = attr(c, "재질");
        if (m == null || m.isBlank()) {
            return 0;
        }

        String mat = m.toLowerCase(Locale.ROOT);
        boolean isSuede = mat.contains("스웨이드") || mat.contains("suede");
        boolean isLeather = mat.contains("레더") || mat.contains("가죽") || mat.contains("leather");

        if ("RAIN".equals(precipitation)) {
            if (isSuede) {
                return -PENALTY_RAIN_SUEDE;
            }
            if (isLeather) {
                return -PENALTY_RAIN_LEATHER;
            }
        } else if ("SNOW".equals(precipitation)) {
            if (isLeather) {
                return -PENALTY_SNOW_LEATHER;
            }
            if (isSuede) {
                return -PENALTY_SNOW_SUEDE;
            }
        }
        return 0;
    }

    // ───── 두께 점수(앵커/체감 연동) ─────

    private int thicknessScore(Clothes c, Clothes anchor) {
        String t = attr(c, "두께"); // "얇음"/"보통"/"두꺼움"
        if (t == null || t.isBlank()) return 0;

        // 1) 기본: 체감온도 기반 가점/감점
        int base = switch (c.getType()) {
            case TOP, DRESS -> scoreTopLike(t, lastPtDay);
            case OUTER      -> scoreOuter(t, lastPtNight);
            default         -> 0; // 하의/신발/모자/머플러 등은 영향 없음
        };

        // 레이어링 상호작용
        int layering = 0;
        if (c.getType() == ClothesType.TOP) {
            if (anchor != null && anchor.getType() == ClothesType.OUTER) {
                String outerThick = attr(anchor, "두께");
                if ("얇음".equals(outerThick)) {
                    // 얇은 아우터 입으면 상의는 조금 더 얇게 OK
                    if ("얇음".equals(t)) layering += 2;
                    else if ("보통".equals(t)) layering += 1;
                } else if ("보통".equals(outerThick)) {
                    // 보통 아우터면 상의 얇음/보통 모두 무난
                    if ("얇음".equals(t) || "보통".equals(t)) layering += 1;
                }
            } else {
                // 아우터 미착용: 경계 구간(18~22°C)에서 '보통'(예: 후드티) 선호
                if (lastPtDay >= 18 && lastPtDay <= 22 && "보통".equals(t)) layering += 2;
            }
        }

        return base + layering;
    }

    // ptDay 구간별 TOP/DRESS 두께 점수
    private int scoreTopLike(String t, double ptDay) {
        if (ptDay >= 27) {
            return switch (t) { case "얇음" -> +5; case "보통" -> -2; case "두꺼움" -> -20; default -> -5; };
        } else if (ptDay >= 23) {
            return switch (t) { case "얇음" -> +4; case "보통" -> +2; case "두꺼움" -> -20; default -> -5; };
        } else if (ptDay >= 18) {
            return switch (t) { case "얇음" -> +2; case "보통" -> +3; case "두꺼움" ->  0; default -> 0; };
        } else if (ptDay >= 12) {
            return switch (t) { case "얇음" -> -2; case "보통" -> +4; case "두꺼움" -> +2; default -> -2; };
        } else {
            return switch (t) { case "얇음" -> -10; case "보통" -> 0; case "두꺼움" -> +4; default -> -5; };
        }
    }

    // ptNight 구간별 OUTER 두께 점수
    private int scoreOuter(String t, double ptNight) {
        if (ptNight > 23) {
            return switch (t) { case "얇음" -> -2; case "보통" -> -5; case "두꺼움" -> -20; default -> -20; };
        } else if (ptNight >= 18) {
            return switch (t) { case "얇음" -> +4; case "보통" -> -2; case "두꺼움" -> -10; default -> -10; };
        } else if (ptNight >= 14) {
            return switch (t) { case "얇음" -> +2; case "보통" -> +3; case "두꺼움" -> -10; default -> -10; };
        } else if (ptNight >= 10) {
            return switch (t) { case "얇음" ->  0; case "보통" -> +4; case "두꺼움" -> -2; default -> 0; };
        } else {
            return switch (t) { case "얇음" -> -10; case "보통" ->  0; case "두꺼움" -> +4; default -> 0; };
        }
    }


    // ────────────────────────────────────────────────────────────────────────
    // 스타일 점수(Map 기반)
    // ────────────────────────────────────────────────────────────────────────

    private int styleScore(String anchor, String item) {
        if (anchor == null || item == null) {
            return STYLE_DEFAULT;
        }
        String a = anchor.trim();
        String b = item.trim();
        if (a.isEmpty() || b.isEmpty()) {
            return STYLE_DEFAULT;
        }

        if (a.equals(b)) {
            return STYLE_EXACT;
        }
        if (STYLE_ANY.equals(a) || STYLE_ANY.equals(b)) {
            return STYLE_COMPAT;
        }
        return isCompatible(a, b) ? STYLE_COMPAT : STYLE_DEFAULT;
    }

    private boolean isCompatible(String a, String b) {
        Set<String> fromA = COMPAT.get(a);
        if (fromA != null && fromA.contains(b)) {
            return true;
        }
        Set<String> fromB = COMPAT.get(b);
        return (fromB != null && fromB.contains(a));
    }

    // ────────────────────────────────────────────────────────────────────────
    // 유틸
    // ────────────────────────────────────────────────────────────────────────

    private String attr(Clothes c, String nameKo) {
        for (ClothesAttribute a : c.getAttributes()) {
            if (a.getDefinition() == null || a.getDefinition().getName() == null) {
                continue;
            }
            if (nameKo.equals(a.getDefinition().getName().trim())) {
                return a.getOptionValue();
            }
        }
        return null;
    }

    private double calculatePersonalTemperature(double temp, double sensitive) {
        return temp + (sensitive - 3);
    }

    private static String seasonByMonthWithTransition(Instant utc, double ptDay) {
        int m = utc.atZone(KST).getMonthValue();
        if (m == 3) {
            if (ptDay <= MARCH_COLD_PT) return "겨울";
            return "봄";
        }
        if (m == 9) {
            if (ptDay >= SEPT_HOT_PT) return "여름";
            return "가을";
        }
        // 나머지는 기존 월 베이스
        return seasonByMonth(utc);
    }

    private static String seasonByMonth(Instant utc) {
        int m = utc.atZone(KST).getMonthValue();
        if (m >= 3 && m <= 5) {
            return "봄";
        }
        if (m >= 6 && m <= 8) {
            return "여름";
        }
        if (m >= 9 && m <= 11) {
            return "가을";
        }
        return "겨울";
    }

    private static double windMs(Weather w) {
        if (w.getWindSpeed() != null) {
            return w.getWindSpeed();
        }
        String ww = enumName(w.getWindSpeedWord());
        if ("WEAK".equals(ww)) {
            return 1.5;
        }
        if ("MODERATE".equals(ww)) {
            return 3.5;
        }
        if ("STRONG".equals(ww)) {
            return 7.0;
        }
        return 0.0;
    }

    private static double nz(Double v, double d) {
        return v == null ? d : v;
    }

    private static String enumName(Object e) {
        return e == null ? null : String.valueOf(e);
    }

    private static Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    private static Instant toInstantSafe(Object ts) {
        if (ts == null) {
            return null;
        }
        if (ts instanceof Instant i) {
            return i;
        }
        try {
            return (Instant) ts.getClass().getMethod("toInstant").invoke(ts);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static <T> T firstNonNull(T a, T b, T c) {
        return a != null ? a : (b != null ? b : c);
    }

    private static void addIfNotNull(List<Clothes> list, Clothes c) {
        if (c != null) {
            list.add(c);
        }
    }
    // ------------------------------------ 예전 메소드 -------------------------------------------

    /**
     * 같은 타입 리스트에서 최고 득점 1개
     */
    private Clothes pickBest(List<Clothes> list, String seasonNow, Clothes anchor, String precipitation, Integer precipitationProb) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        String anchorStyle = (anchor == null) ? null : attr(anchor, "스타일");
        int best = 0;
        Clothes bestItem = null;
        for (Clothes c : list) {
            int s = totalScore(c, seasonNow, anchorStyle, precipitation, precipitationProb, anchor);
            if (s > best) {
                best = s;
                bestItem = c;
            }
        }
        return bestItem;
    }

    // ────────────────────────────────────────────────────────────────────────
    // 필터: 계절/두께
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 계절 필터: 봄&가을, 여름, 겨울, 사계절로 구분 (사계절은 모든 계절에 통과)
     */
    private boolean seasonAllowedStrict(Clothes c, String now) {
        String s = attr(c, "계절");
        if (s == null || s.isBlank()) {
            return false;
        }
        if ("사계절".equals(s)) {
            return true;
        }

        return switch (now) {
            case "봄", "가을" -> ("봄".equals(s) || "가을".equals(s));
            case "여름" -> "여름".equals(s);
            case "겨울" -> "겨울".equals(s);
            default -> true;
        };
    }

    private int seasonPreference(String now, String item) {
        if (item == null || item.isBlank()) {
            return 0;
        }
        if ("사계절".equals(item)) {
            return SEASON_PREF_ALL;
        }
        if (item.equals(now)) {
            return SEASON_PREF_EXACT;
        }
        if (("봄".equals(now) && "가을".equals(item)) || ("가을".equals(now) && "봄".equals(item))) {
            return SEASON_PREF_PAIR;
        }
        return 0;
    }

    /**
     * 두께 필터: OUTER=ptNight 기준, TOP=ptDay 기준 (없으면 통과)
     */
    private boolean thicknessAllowed(Clothes c, double ptDay, double ptNight) {
        String t = attr(c, "두께"); // 얇음/보통/두꺼움
        if (t == null || t.isBlank()) {
            return true;
        }

        if (c.getType() == ClothesType.OUTER) {
            if (ptNight > 21) {
                return false;
            }
            if (ptNight > 18) {
                return t.equals("얇음");
            }
            if (ptNight > 15) {
                return t.equals("얇음") || t.equals("보통");
            }
            if (ptNight >= 9) {
                return t.equals("보통");
            }
            return t.equals("두꺼움");
        }
        if (c.getType() == ClothesType.TOP) {
            if (ptDay > 26) {
                return t.equals("얇음");
            }
            if (ptDay > 23) {
                return t.equals("얇음") || t.equals("보통");
            }
            if (ptDay > 8) {
                return t.equals("보통");
            }
            return t.equals("보통") || t.equals("두꺼움");
        }
        return true;
    }
}
