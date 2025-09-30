package org.ikuzo.otboo.domain.recommendation.service.engine.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttribute;
import org.ikuzo.otboo.domain.clothes.enums.ClothesType;
import org.ikuzo.otboo.domain.clothes.repository.ClothesRepository;
import org.ikuzo.otboo.domain.recommendation.service.engine.RecommendationEngine;
import org.ikuzo.otboo.domain.recommendation.support.KmaPerceivedTemperature;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreBasedRecommendationEngine implements RecommendationEngine {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ClothesRepository clothesRepository;

    // 아우터 추천 기준
    private static final double OUTER_NEED_NIGHT_COOL = 20.0; // ptNight ≤ 20 → 아우터 필요
    private static final double OUTER_NEED_CURRENT_HOT = 23.0; // 현재 온도를 기반으로 아우터 판단

    // 과도기 온도 기준
    private static final double MARCH_COLD_PT = 12.0;  // 이하면 겨울로 보는 쪽
    private static final double SEPT_HOT_PT = 25.0;  // 이상이면 여름으로 보는 쪽

    // 계절 약가점
    private static final int SEASON_PREF_EXACT = 3;  // 현재 계절 정확 일치
    private static final int SEASON_PREF_ALL = 1;    // 사계절
    private static final int SEASON_PREF_PAIR = 2;   // 봄↔가을 교차 및 약한 인접 가점

    // 타입별 추천 최소 총점(플로어)
    private static final int FLOOR_PRIMARY = 2; // TOP/DRESS
    private static final int FLOOR_BOTTOM = 1; // BOTTOM
    private static final int FLOOR_OUTER_REQUIRED = 2; // OUTER
    private static final int FLOOR_MISC = 2; // SHOES/HAT/SCARF/ACCESSORY

    // 스타일 점수
    private static final int STYLE_EXACT = 5;
    private static final int STYLE_COMPAT = 3;
    private static final int STYLE_DEFAULT = 0;
    private static final String STYLE_ANY = "기본";

    // 재질 감점(강수 확률 ≥50%)
    private static final int PENALTY_RAIN_SUEDE = 3;
    private static final int PENALTY_RAIN_LEATHER = 2;
    private static final int PENALTY_SNOW_SUEDE = 3;
    private static final int PENALTY_SNOW_LEATHER = 2;

    // 동률/근접 후보 내 다양성
    private static final int NEAR_BEST_DELTA = 2;

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
        final double ptNight = calculatePersonalTemperature(kmaTmpNight, personalSensitivity);

        final String seasonNow = seasonByMonthWithTransition(forecastUtc, ptDay);
        final String precipitation = enumName(weather.getPrecipitationType());
        final Integer precipitationProb = toInt(weather.getPrecipitationProbability());

        final boolean outerNeeded = isOuterNeeded(ptDay, ptNight);

        log.info("ptDay: {}, ptNight: {}, seasonNow: {}", ptDay, ptNight, seasonNow);

        List<Clothes> all = clothesRepository.findByOwnerId(owner.getId());
        if (all.isEmpty()) {
            return List.of();
        }

        Map<ClothesType, List<Clothes>> byType = all.stream()
            .collect(Collectors.groupingBy(Clothes::getType));

        if (outerNeeded) {
            log.info("아우터 점수 계산 실시");
            Clothes outer = pickBestWithFloor(byType.get(ClothesType.OUTER), seasonNow, null,
                precipitation, precipitationProb, FLOOR_OUTER_REQUIRED, ptDay, ptNight);

            if (outer == null) {
                log.info("아우터 필요하지만 후보 미달 - OUTER 없이 진행");
                return pickWithoutOuter(byType, seasonNow, precipitation, precipitationProb, ptDay, ptNight);
            }

            List<Clothes> result = new ArrayList<>();
            result.add(outer);

            log.info("상의/드레스 점수 계산 실시");
            Clothes top = pickBestWithFloor(byType.get(ClothesType.TOP), seasonNow, outer,
                precipitation, precipitationProb, FLOOR_PRIMARY, ptDay, ptNight);
            Clothes dress = pickBestWithFloor(byType.get(ClothesType.DRESS), seasonNow, outer,
                precipitation, precipitationProb, FLOOR_PRIMARY, ptDay, ptNight);
            log.info("상의 vs 드레스 비교");
            Clothes inner = betterOf(top, dress, seasonNow, outer, precipitation, precipitationProb, ptDay, ptNight);
            log.info("아우터의 inner로 골라진 첫 의상 - {}", inner == null ? null : inner.getName());

            if (inner != null) {
                result.add(inner);
                if (inner.getType() == ClothesType.TOP) {
                    log.info("하의 점수 계산 실시");
                    Clothes bottom = pickBestWithFloor(byType.get(ClothesType.BOTTOM), seasonNow,
                        outer, precipitation, precipitationProb, FLOOR_BOTTOM, ptDay, ptNight);
                    if (bottom != null) {
                        result.add(bottom);
                    }
                }
            } else {
                log.info("상의/드레스 없음 - 하의 점수 계산 실시");
                Clothes bottom = pickBestWithFloor(byType.get(ClothesType.BOTTOM), seasonNow, outer,
                    precipitation, precipitationProb, FLOOR_BOTTOM, ptDay, ptNight);
                if (bottom != null) {
                    result.add(bottom);
                }
            }

            log.info("신발 점수 계산 실시");
            addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.SHOES), seasonNow, outer,
                precipitation, precipitationProb, FLOOR_MISC, ptDay, ptNight));
            log.info("모자 점수 계산 실시");
            addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.HAT), seasonNow, outer,
                precipitation, precipitationProb, FLOOR_MISC, ptDay, ptNight));
            log.info("스카프 점수 계산 실시");
            addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.SCARF), seasonNow, outer,
                precipitation, precipitationProb, FLOOR_MISC, ptDay, ptNight));
            log.info("악세서리 점수 계산 실시");
            addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.ACCESSORY), seasonNow, outer,
                    precipitation, precipitationProb, FLOOR_MISC, ptDay, ptNight));

            return result;
        }

        log.info("아우터 불필요");
        return pickWithoutOuter(byType, seasonNow, precipitation, precipitationProb, ptDay, ptNight);
    }

    // ───────── 내부 선택 로직 ─────────

    /**
     * 저녁 온도 < 20 || 현재 온도 < 23 이면 아우터가 필요한 상황으로 판정
     */
    private boolean isOuterNeeded(double ptDay, double ptNight) {
        return ptNight <= OUTER_NEED_NIGHT_COOL || ptDay <= OUTER_NEED_CURRENT_HOT;
    }

    /**
     * 아우터가 불필요한 경우의 의상 추천 로직
     */
    private List<Clothes> pickWithoutOuter(
        Map<ClothesType, List<Clothes>> byType,
        String seasonNow,
        String precipitation,
        Integer precipitationProb,
        double ptDay,
        double ptNight
    ) {
        List<Clothes> result = new ArrayList<>();

        log.info("상의/드레스 점수 계산 실시");
        Clothes topCandidate = pickBestWithFloor(byType.get(ClothesType.TOP), seasonNow, null,
            precipitation, precipitationProb, FLOOR_PRIMARY, ptDay, ptNight);
        Clothes dressCandidate = pickBestWithFloor(byType.get(ClothesType.DRESS), seasonNow, null,
            precipitation, precipitationProb, FLOOR_PRIMARY, ptDay, ptNight);

        log.info("모든 상의 후보 수: {}", byType.getOrDefault(ClothesType.TOP, List.of()).size());
        log.info("상의 - {}, 드레스 - {}", topCandidate.getName(), dressCandidate.getName());

        Clothes primary = betterOf(topCandidate, dressCandidate, seasonNow, null,
            precipitation, precipitationProb, ptDay, ptNight);
        log.info("아우터가 필요 없는 상황에서 골라진 첫 의상 - {}", primary == null ? null : primary.getName());

        if (primary != null) {
            result.add(primary);
            if (primary.getType() == ClothesType.TOP) {
                log.info("하의 점수 계산 실시");
                Clothes bottom = pickBestWithFloor(byType.get(ClothesType.BOTTOM), seasonNow,
                    primary,
                    precipitation, precipitationProb, FLOOR_BOTTOM, ptDay, ptNight);
                if (bottom != null) {
                    result.add(bottom);
                }
            }
        } else {
            Clothes bottom = pickBestWithFloor(byType.get(ClothesType.BOTTOM), seasonNow, null,
                precipitation, precipitationProb, FLOOR_BOTTOM, ptDay, ptNight);
            if (bottom != null) {
                result.add(bottom);
            }
        }

        log.info("신발 점수 계산 실시");
        addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.SHOES), seasonNow, primary,
            precipitation, precipitationProb, FLOOR_MISC, ptDay, ptNight));
        log.info("모자 점수 계산 실시");
        addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.HAT), seasonNow, primary,
            precipitation, precipitationProb, FLOOR_MISC, ptDay, ptNight));
        log.info("스카프 점수 계산 실시");
        addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.SCARF), seasonNow, primary,
            precipitation, precipitationProb, FLOOR_MISC, ptDay, ptNight));
        log.info("악세서리 점수 계산 실시");
        addIfNotNull(result, pickBestWithFloor(byType.get(ClothesType.ACCESSORY), seasonNow, primary,
            precipitation, precipitationProb, FLOOR_MISC, ptDay, ptNight));

        return result;
    }

    /**
     * 최고 점수를 받은 의상을 기준으로 1점 이내의 의상들을 랜덤 추천
     * 임계점을 통과 못 하면 해당 카테고리는 추천하지 않음
     */
    private Clothes pickBestWithFloor(
        List<Clothes> list,
        String seasonNow,
        Clothes anchor,
        String precipitation,
        Integer precipitationProb,
        int floorTotal,
        double ptDay,
        double ptNight
    ) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        String anchorStyle = (anchor == null) ? null : attr(anchor, "스타일");

        List<Clothes> passed = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        int bestPassed = Integer.MIN_VALUE;

        for (Clothes c : list) {
            int s = totalScore(c, seasonNow, anchorStyle, precipitation, precipitationProb, anchor,
                ptDay, ptNight);
            if (s >= floorTotal) {
                passed.add(c);
                scores.add(s);
                if (s > bestPassed) {
                    bestPassed = s;
                }
            }
        }
        if (passed.isEmpty()) {
            return null;
        }

        int cutoff = bestPassed - NEAR_BEST_DELTA;
        List<Clothes> pool = new ArrayList<>();
        for (int i = 0; i < passed.size(); i++) {
            if (scores.get(i) >= cutoff) {
                pool.add(passed.get(i));
            }
        }

        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    /**
     * 총점 = (계절 점수) + (스타일 점수) + (재질×강수 감점) + (두께 점수)
     */
    private int totalScore(
        Clothes c,
        String seasonNow,
        String anchorStyle,
        String precipitation,
        Integer precipitationProb,
        Clothes anchor,
        double ptDay,
        double ptNight
    ) {
        String itemSeason = attr(c, "계절");
        String itemStyle = attr(c, "스타일");

        int sSeason = seasonAffinityScore(seasonNow, itemSeason);
        int sStyle = styleScore(anchorStyle, itemStyle);
        int sPenalty = materialPenalty(c, precipitation, precipitationProb);
        int sThick = thicknessScore(c, anchor, ptDay, ptNight);

        int total = sSeason + sStyle + sPenalty + sThick;
        log.info("의상: {}, 점수 - Season:{}, Style:{}, Penalty:{}, Thick:{}, Total:{}",
            c.getName(), sSeason, sStyle, sPenalty, sThick, total);
        return total;
    }

    /**
     * TOP vs DRESS 중 더 높은 점수 선택
     */
    private Clothes betterOf(
        Clothes a,
        Clothes b,
        String seasonNow,
        Clothes anchor,
        String precipitation,
        Integer precipitationProb,
        double ptDay,
        double ptNight
    ) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;

        String anchorStyle = (anchor == null) ? null : attr(anchor, "스타일");
        int sa = totalScore(a, seasonNow, anchorStyle, precipitation, precipitationProb, anchor, ptDay, ptNight);
        int sb = totalScore(b, seasonNow, anchorStyle, precipitation, precipitationProb, anchor, ptDay, ptNight);

        if (Math.abs(sa - sb) <= NEAR_BEST_DELTA) {
            return ThreadLocalRandom.current().nextBoolean() ? a : b;
        }

        return (sa >= sb) ? a : b;
    }

    /**
     * 계절 가점(정확 +3, 인접/교차 +2, 사계절 +1, 그 외 0)
     */
    private int seasonAffinityScore(String now, String item) {
        if (item == null || item.isBlank()) {
            return 0;
        }
        if ("사계절".equals(item)) {
            return SEASON_PREF_ALL;
        }
        if (item.equals(now)) {
            return SEASON_PREF_EXACT;
        }
        boolean weakPair =
            ("봄".equals(now) && ("가을".equals(item) || "겨울".equals(item))) ||
                ("가을".equals(now) && ("봄".equals(item) || "여름".equals(item))) ||
                ("여름".equals(now) && "가을".equals(item)) ||
                ("겨울".equals(now) && "봄".equals(item));
        return weakPair ? SEASON_PREF_PAIR : 0;
    }

    /**
     * 재질 감점(비&눈: 스웨이드, 가죽 감점)
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

    /**
     * 두께 가점(아우터와 상의는 현재 온도에 따른 적절성 판정)
     */
    private int thicknessScore(Clothes c, Clothes anchor, double ptDay, double ptNight) {
        String t = attr(c, "두께"); // "얇음"/"보통"/"두꺼움"
        if (t == null || t.isBlank()) {
            return 0;
        }

        int base = switch (c.getType()) {
            case TOP, DRESS -> scoreTopLike(t, ptDay);
            case OUTER -> scoreOuter(t, ptNight);
            default -> 0;
        };

        int layering = 0;
        if (c.getType() == ClothesType.TOP) {
            if (anchor != null && anchor.getType() == ClothesType.OUTER) {
                String outerThick = attr(anchor, "두께");
                if ("얇음".equals(outerThick)) {
                    if ("얇음".equals(t)) {
                        layering += 2;
                    } else if ("보통".equals(t)) {
                        layering += 1;
                    }
                } else if ("보통".equals(outerThick)) {
                    if ("얇음".equals(t) || "보통".equals(t)) {
                        layering += 1;
                    }
                }
            } else {
                if (ptDay >= 18 && ptDay <= 22 && "보통".equals(t)) {
                    layering += 2;
                }
            }
        }
        return base + layering;
    }

    /**
     * ptDay 구간별 TOP/DRESS 두께 점수
     */
    private int scoreTopLike(String t, double ptDay) {
        if (ptDay >= 27) {
            return switch (t) {
                case "얇음" -> +5;
                case "보통" -> -2;
                case "두꺼움" -> -20;
                default -> -5;
            };
        } else if (ptDay >= 23) {
            return switch (t) {
                case "얇음" -> +4;
                case "보통" -> +2;
                case "두꺼움" -> -20;
                default -> -5;
            };
        } else if (ptDay >= 18) {
            return switch (t) {
                case "얇음" -> +2;
                case "보통" -> +3;
                case "두꺼움" -> 0;
                default -> 0;
            };
        } else if (ptDay >= 12) {
            return switch (t) {
                case "얇음" -> -2;
                case "보통" -> +4;
                case "두꺼움" -> +2;
                default -> -2;
            };
        } else {
            return switch (t) {
                case "얇음" -> -10;
                case "보통" -> 0;
                case "두꺼움" -> +4;
                default -> -5;
            };
        }
    }

    /**
     * ptDay 구간별 OUTER 두께 점수
     */
    private int scoreOuter(String t, double ptNight) {
        if (ptNight > 23) {
            return switch (t) {
                case "얇음" -> -2;
                case "보통" -> -7;
                case "두꺼움" -> -20;
                default -> -20;
            };
        } else if (ptNight >= 18) {
            return switch (t) {
                case "얇음" -> +4;
                case "보통" -> -2;
                case "두꺼움" -> -10;
                default -> -10;
            };
        } else if (ptNight >= 14) {
            return switch (t) {
                case "얇음" -> +2;
                case "보통" -> +3;
                case "두꺼움" -> -10;
                default -> -10;
            };
        } else if (ptNight >= 10) {
            return switch (t) {
                case "얇음" -> 0;
                case "보통" -> +4;
                case "두꺼움" -> -2;
                default -> 0;
            };
        } else {
            return switch (t) {
                case "얇음" -> -10;
                case "보통" -> 0;
                case "두꺼움" -> +4;
                default -> 0;
            };
        }
    }

    /**
     * 매칭 스타일 점수(비슷한 스타일의 의상은 가점)
     */
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

    // ─────내부 유틸 메소드─────

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
            return (ptDay <= MARCH_COLD_PT) ? "겨울" : "봄";
        }
        if (m == 9) {
            return (ptDay >= SEPT_HOT_PT) ? "여름" : "가을";
        }
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
}
