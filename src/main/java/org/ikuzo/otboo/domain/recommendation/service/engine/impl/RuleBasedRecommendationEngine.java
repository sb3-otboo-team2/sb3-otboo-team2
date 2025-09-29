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

    // ===== 튜닝 상수 =====
    private static final double OUTER_NEED_NIGHT_COOL = 21.0; // ptNight ≤ 20 → 아우터 필요
    private static final double OUTER_NEED_NIGHT_MILD = 23.0; // (ptDay ≥ 25 && ptNight ≤ 23) → 필요
    private static final double DAY_HOT_PT = 25.0; // 낮 덥다 판단 기준

    // (선택) 계절 약가점
    private static final int SEASON_PREF_EXACT = 2;  // 현재 계절 정확 일치
    private static final int SEASON_PREF_ALL = 0;  // 사계절
    private static final int SEASON_PREF_PAIR = 1;  // 봄↔가을 교차

    // ===== 스타일 점수(맵 기반) =====
    private static final int STYLE_EXACT = 10; // 동일 스타일
    private static final int STYLE_COMPAT = 8; // 호환/공용
    private static final int STYLE_DEFAULT = 0; // 그 외
    private static final String STYLE_ANY = "기본";

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

    // ===== 재질 감점(강수 확률 ≥50%) =====
    private static final int PENALTY_RAIN_SUEDE = 4; // 비+스웨이드
    private static final int PENALTY_RAIN_LEATHER = 3; // 비+가죽/레더
    private static final int PENALTY_SNOW_SUEDE = 2; // 눈+스웨이드
    private static final int PENALTY_SNOW_LEATHER = 2; // 눈+가죽/레더


    @Override
    public List<Clothes> recommend(User owner, Weather weather) {

        List<Clothes> userClothes = clothesRepository.findByOwnerId(owner.getId());

        final double ta = nz(weather.getTemperatureCurrent(), 20.0);
        final double rh = nz(weather.getHumidityCurrent(), 50.0);
        final double wind = windMs(weather);
        final double minC = nz(weather.getTemperatureMin(), ta);

        final Instant forecastUtc = firstNonNull(
            toInstantSafe(weather.getForecastAt()),
            toInstantSafe(weather.getCreatedAt()),
            Instant.now()
        );

        final double ptDay = KmaPerceivedTemperature.compute(ta, rh, wind, forecastUtc);
        final double ptNight = KmaPerceivedTemperature.compute(minC, rh, wind, forecastUtc);
        final String seasonNow = seasonByMonth(forecastUtc);               // "봄/여름/가을/겨울"
        final String precipitation = enumName(weather.getPrecipitationType()); // "RAIN"/"SNOW"/"NONE"...
        final Integer precipitationProb = toInt(weather.getPrecipitationProbability()); // null 가능

        final boolean outerNeeded = isOuterNeeded(ptDay, ptNight);

        // 후보 필터: 계절 하드 + 두께(OUTER=ptNight, TOP=ptDay)
        List<Clothes> candidateClothes = userClothes.stream()
            .filter(c -> seasonAllowedStrict(c, seasonNow))
            .filter(c -> thicknessAllowed(c, ptDay, ptNight))
            .toList();

        if (candidateClothes.isEmpty()) {
            return List.of();
        }

        Map<ClothesType, List<Clothes>> byType = candidateClothes.stream()
            .collect(Collectors.groupingBy(Clothes::getType));

        if (outerNeeded) {
            log.info("아우터 필요");
            Clothes outer = pickBest(byType.get(ClothesType.OUTER), seasonNow, null, precipitation, precipitationProb);

            if (outer == null) {
                log.info("아우터는 필요하지만 아우터가 없음");
                return pickWithoutOuter(byType, seasonNow, precipitation, precipitationProb);
            }

            List<Clothes> result = new ArrayList<>();
            result.add(outer);

            Clothes top = pickBest(byType.get(ClothesType.TOP), seasonNow, outer, precipitation, precipitationProb);
            Clothes dress = pickBest(byType.get(ClothesType.DRESS), seasonNow, outer, precipitation, precipitationProb);
            Clothes inner = betterOf(top, dress, seasonNow, outer, precipitation, precipitationProb);

            if (inner != null) {
                result.add(inner);
                if (inner.getType() == ClothesType.TOP) {
                    log.info("상의가 골라짐");
                    Clothes bottom = pickBest(byType.get(ClothesType.BOTTOM), seasonNow, outer,
                        precipitation, precipitationProb);
                    if (bottom != null) {
                        result.add(bottom);
                    }
                }
            } else {
                log.info("상의 또는 드레스가 존재하지 않음");
                Clothes bottom = pickBest(byType.get(ClothesType.BOTTOM), seasonNow, outer, precipitation,
                    precipitationProb);
                if (bottom != null) {
                    result.add(bottom);
                }
            }
            addIfNotNull(result,
                pickBest(byType.get(ClothesType.SHOES), seasonNow, outer, precipitation,
                    precipitationProb));
            addIfNotNull(result,
                pickBest(byType.get(ClothesType.HAT), seasonNow, outer, precipitation,
                    precipitationProb));
            addIfNotNull(result,
                pickBest(byType.get(ClothesType.SCARF), seasonNow, outer, precipitation,
                    precipitationProb));

            return result;
        }

        log.info("아우터 불필요");
        return pickWithoutOuter(byType, seasonNow, precipitation, precipitationProb);

    }

    // ────────────────────────────────────────────────────────────────────────
    // 내부 선택 로직
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 아우터 필요: (1) 밤 체감 ≤ 21°C, (2) 낮 덥고(≥25) 밤 ≤ 23°C
     */
    private boolean isOuterNeeded(double ptDay, double ptNight) {
        if (ptNight <= OUTER_NEED_NIGHT_COOL) {
            return true;
        }
        if (ptDay >= DAY_HOT_PT && ptNight <= OUTER_NEED_NIGHT_MILD) {
            return true;
        }
        return false;
    }

    private List<Clothes> pickWithoutOuter(Map<ClothesType, List<Clothes>> byType,
        String seasonNow, String precipitation, Integer precipitationProb
    ) {
        List<Clothes> result = new ArrayList<>();

        Clothes topCandidate = pickBest(byType.get(ClothesType.TOP), seasonNow, null, precipitation, precipitationProb);
        Clothes dressCandidate = pickBest(byType.get(ClothesType.DRESS), seasonNow, null, precipitation, precipitationProb);

        log.info("상의 - {}, 드레스 - {}", topCandidate, dressCandidate);
        Clothes primary = betterOf(topCandidate, dressCandidate, seasonNow, null,
            precipitation, precipitationProb);

        log.info("아우터가 필요 없는 상황에서 골라진 옷 - {}", primary);
        if (primary != null) {
            result.add(primary);
            if (primary.getType() == ClothesType.TOP) {
                Clothes bottom = pickBest(byType.get(ClothesType.BOTTOM), seasonNow, primary, precipitation, precipitationProb);
                if (bottom != null) {
                    result.add(bottom);
                }
            }
        } else {
            Clothes bottom = pickBest(byType.get(ClothesType.BOTTOM), seasonNow, null, precipitation, precipitationProb);
            if (bottom != null) {
                result.add(bottom);
            }
        }

        addIfNotNull(result,
            pickBest(byType.get(ClothesType.OUTER), seasonNow, primary, precipitation,
                precipitationProb));
        addIfNotNull(result,
            pickBest(byType.get(ClothesType.SHOES), seasonNow, primary, precipitation,
                precipitationProb));
        addIfNotNull(result,
            pickBest(byType.get(ClothesType.HAT), seasonNow, primary, precipitation,
                precipitationProb));
        addIfNotNull(result,
            pickBest(byType.get(ClothesType.SCARF), seasonNow, primary, precipitation,
                precipitationProb));
        return result;
    }

    /**
     * 같은 타입 리스트에서 최고 득점 1개
     */
    private Clothes pickBest(List<Clothes> list, String seasonNow, Clothes anchor, String precipitation,
        Integer precipitationProb
    ) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        String anchorStyle = (anchor == null) ? null : attr(anchor, "스타일");
        int best = Integer.MIN_VALUE;
        Clothes bestItem = null;
        for (Clothes c : list) {
            int s = totalScore(c, seasonNow, anchorStyle, precipitation, precipitationProb);
            if (s > best) {
                best = s;
                bestItem = c;
            }
        }
        return bestItem;
    }

    /**
     * 총점 = (계절 약가점) + (스타일 점수) + (재질×강수 감점)
     */
    private int totalScore(Clothes c, String seasonNow, String anchorStyle,
        String precipitation, Integer precipitationProb) {
        String itemSeason = attr(c, "계절");
        String itemStyle = attr(c, "스타일");
        int sSeason = seasonPreference(seasonNow, itemSeason);
        int sStyle = styleScore(anchorStyle, itemStyle);
        int sPenalty = materialPenalty(c, precipitation, precipitationProb);

        log.info("의상: {}, 점수 - Season: {}, Style: {}, Penalty: {}", c.getName(), sSeason , sStyle , sPenalty);
        return sSeason + sStyle + sPenalty;
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
        int sa = totalScore(a, seasonNow, anchorStyle, precipitation, precipitationProb);
        int sb = totalScore(b, seasonNow, anchorStyle, precipitation, precipitationProb);
        return (sa >= sb) ? a : b;
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
            if (ptNight > 17) {
                return t.equals("얇음") || t.equals("보통");
            }
            if (ptNight >= 9) {
                return t.equals("보통");
            }
            return t.equals("두꺼움");
        }
        if (c.getType() == ClothesType.TOP) {
            if (ptDay > 23) {
                return t.equals("얇음");
            }
            if (ptDay > 17) {
                return t.equals("얇음") || t.equals("보통");
            }
            if (ptDay > 10) {
                return t.equals("보통");
            }
            return t.equals("보통") || t.equals("두꺼움");
        }
        return true;
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
}
