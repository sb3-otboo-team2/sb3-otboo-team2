package org.ikuzo.otboo.domain.weather.service;

import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.notification.entity.Level;
import org.ikuzo.otboo.domain.notification.service.NotificationService;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.ikuzo.otboo.domain.weather.repository.WeatherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherAlertServiceImpl implements WeatherAlertService {

    private final WeatherRepository weatherRepository;
    private final NotificationService notificationService;

    /**
     * 강수(PTY != NONE)로 바뀐 경우
     */
    @Transactional(readOnly = true)
    @Override
    public void checkAndNotify(User user, Weather latest) {
        Optional<Weather> prevOpt = weatherRepository
            .findTop1ByUserAndForecastAtLessThanOrderByForecastAtDesc(user, latest.getForecastAt());
        if (prevOpt.isEmpty()) {
            log.debug("[WeatherAlertService] 사용자 {}: 이전 예보 없음 → 알림 미발송", user.getId());
            return;
        }

        Weather prev = prevOpt.get();
        log.debug("[WeatherAlertService] 사용자 {}: 이전 기온={}, 현재 기온={}, 이전 강수={}, 현재 강수={}",
            user.getId(), prev.getTemperatureCurrent(), latest.getTemperatureCurrent(),
            prev.getPrecipitationType(), latest.getPrecipitationType());
        // 강수 시작
        boolean startedRaining = !"NONE".equals(latest.getPrecipitationType())
            && ("NONE".equals(prev.getPrecipitationType()));

        // 급격한 기온 변화 (기본 5℃)
        boolean tempJump = false;
        if (latest.getTemperatureCurrent() != null && prev.getTemperatureCurrent() != null) {
            tempJump = Math.abs(latest.getTemperatureCurrent() - prev.getTemperatureCurrent()) >= 5.0;
        }

        if (startedRaining) {
            log.info("[WeatherAlertService] 사용자 {}: 강수 시작 감지 → 알림 발송 예정", user.getId());
            notificationService.create(
                Set.of(user.getId()),
                "비 소식 알림",
                "곧 비가 시작됩니다. 우산을 챙겨 주세요!",
                Level.WARNING
            );
        } else if (tempJump) {
            log.info("[WeatherAlertService] 사용자 {}: 기온 급격 변화 감지 (Δ={}℃) → 알림 발송 예정",
                user.getId(), latest.getTemperatureCurrent() - prev.getTemperatureCurrent());
            String msg = (latest.getTemperatureCurrent() - prev.getTemperatureCurrent()) > 0
                ? "기온이 급상승 중입니다. 가벼운 옷차림을 고려하세요."
                : "기온이 급하강 중입니다. 겉옷을 준비하세요.";
            notificationService.create(
                Set.of(user.getId()),
                "급격한 기온 변화 알림",
                msg,
                Level.WARNING
            );
        }
    }
}