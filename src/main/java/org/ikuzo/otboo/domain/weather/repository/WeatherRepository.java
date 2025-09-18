package org.ikuzo.otboo.domain.weather.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherRepository extends JpaRepository<Weather, UUID> {
    Optional<Weather> findTop1ByUserOrderByForecastAtDesc(User user);

    Optional<Weather> findTop1ByUserAndForecastAtLessThanOrderByForecastAtDesc(User user, Instant forecastAt);
}