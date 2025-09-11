package org.ikuzo.otboo.domain.weather.repository;

import java.util.UUID;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherRepository extends JpaRepository<Weather, UUID> {
}
