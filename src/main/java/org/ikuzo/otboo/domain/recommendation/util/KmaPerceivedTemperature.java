package org.ikuzo.otboo.domain.recommendation.util;

import java.time.Instant;
import java.time.ZoneId;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class KmaPerceivedTemperature {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * @param taC           현재기온(°C)
     * @param rh            상대습도(%)
     * @param windMs        풍속(m/s) - 겨울식에서 km/h로 변환
     * @param forecastUtc 예보시각(UTC)
     */
    public static double compute(double taC, double rh, double windMs, Instant forecastUtc) {
        int monthKst = forecastUtc.atZone(KST).getMonthValue();
        boolean isSummer = (monthKst >= 5 && monthKst <= 9);

        if (isSummer) {
            // 여름: -0.2442 + 0.55399*Tw + 0.45535*Ta – 0.0022*Tw^2 + 0.00278*Tw*Ta + 3.0
            double tw = wetBulbTempStull(taC, rh);
            return -0.2442 + 0.55399 * tw + 0.45535 * taC - 0.0022 * (tw * tw) + 0.00278 * tw * taC
                + 3.0;
        } else {
            // 겨울: 바람체감 (조건: Ta ≤ 10°C && V ≥ 1.3 m/s)
            if (taC <= 10.0 && windMs >= 1.3) {
                double vKmh = windMs * 3.6;
                double v016 = Math.pow(vKmh, 0.16);
                return 13.12 + 0.6215 * taC - 11.37 * v016 + 0.3965 * taC * v016;
            }
            return taC;
        }
    }

    /**
     * Stull(2011) 근사식: Ta(°C), RH(%) → Tw(°C)
     */
    public static double wetBulbTempStull(double t, double rh) {
        rh = clamp(rh);
        return t * Math.atan(0.151977 * Math.sqrt(rh + 8.313659))
            + Math.atan(t + rh)
            - Math.atan(rh - 1.676331)
            + 0.00391838 * Math.pow(rh, 1.5) * Math.atan(0.023101 * rh)
            - 4.686035;
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(100.0, v));
    }

}
