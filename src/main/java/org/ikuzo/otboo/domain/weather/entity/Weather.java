package org.ikuzo.otboo.domain.weather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ikuzo.otboo.global.base.BaseUpdatableEntity;

@Entity
@Table(name = "weathers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Weather extends BaseUpdatableEntity {

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;

    @Column(name = "forecasted_at", nullable = false)
    private ZonedDateTime forecastedAt;

    @Column(name = "forecast_at", nullable = false)
    private ZonedDateTime forecastAt;

    @Column(name = "sky_status", nullable = false, length = 20)
    private String skyStatus; // CHECK (CLEAR, MOSTLY_CLOUDY, CLOUDY)

    @Column(name = "precipitation_type", nullable = false, length = 20)
    private String precipitationType; // CHECK (NONE, RAIN, RAIN_SNOW, SNOW, SHOWER)

    @Column(name = "precipitation_amount")
    private Double precipitationAmount;

    @Column(name = "precipitation_probability", nullable = false)
    private Double precipitationProbability;

    @Column(name = "temperature_current", nullable = false)
    private Double temperatureCurrent;

    @Column(name = "temperature_compared")
    private Double temperatureCompared;

    @Column(name = "temperature_min")
    private Double temperatureMin;

    @Column(name = "temperature_max")
    private Double temperatureMax;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(name = "wind_speed_word", length = 20)
    private String windSpeedWord; // CHECK (WEAK, MODERATE, STRONG)

    @Column(name = "humidity_current")
    private Double humidityCurrent;

    @Column(name = "humidity_compared")
    private Double humidityCompared;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = ZonedDateTime.now();
    }
}