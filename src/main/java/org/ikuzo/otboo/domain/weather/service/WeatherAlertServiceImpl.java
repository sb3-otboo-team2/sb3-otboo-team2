package org.ikuzo.otboo.domain.weather.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.ikuzo.otboo.domain.weather.repository.WeatherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeatherAlertServiceImpl implements WeatherAlertService {

    private final WeatherRepository weatherRepository;

    /**
     * 간단 정책: - 강수(PTY != NONE)로 바뀐 경우 - 3시간 내 예보에서 기온 변화 절대값 ≥ 5℃
     */
    @Transactional(readOnly = true)
    @Override
    public void checkAndNotify(User user, Weather latest) {
        Optional<Weather> prevOpt = weatherRepository.findTop1ByUserOrderByForecastAtDesc(user);
        if (prevOpt.isEmpty()) {
            return;
        }

        Weather prev = prevOpt.get();

        // 강수 시작
        boolean startedRaining = !"NONE".equals(latest.getPrecipitationType())
            && ("NONE".equals(prev.getPrecipitationType()));

        // 급격한 기온 변화 (기본 5℃)
        boolean tempJump = false;
        if (latest.getTemperatureCurrent() != null && prev.getTemperatureCurrent() != null) {
            tempJump = Math.abs(latest.getTemperatureCurrent() - prev.getTemperatureCurrent()) >= 5.0;
        }

        if (startedRaining) {
//            notificationService.notifyWarning(user, "비 소식 알림", "곧 비가 올 예정입니다. 우산을 챙기세요!");
        } else if (tempJump) {
            String msg = (latest.getTemperatureCurrent() - prev.getTemperatureCurrent()) > 0
                ? "기온이 급상승 중입니다. 가벼운 옷차림을 고려하세요."
                : "기온이 급하강 중입니다. 겉옷을 준비하세요.";
//            notificationService.notifyWarning(user, "급격한 기온 변화 알림", msg);
        }
    }
}