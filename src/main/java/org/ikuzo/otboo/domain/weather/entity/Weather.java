package org.ikuzo.otboo.domain.weather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.global.base.BaseEntity;

@Entity
@Table(name = "weathers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Weather extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "forecasted_at", nullable = false)
    private Instant forecastedAt;

    @Column(name = "forecast_at", nullable = false)
    private Instant forecastAt;

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

    public void updateFrom(Weather source) {
        this.forecastedAt = source.forecastedAt;
        this.forecastAt = source.forecastAt;
        this.skyStatus = source.skyStatus;
        this.precipitationType = source.precipitationType;
        this.precipitationAmount = source.precipitationAmount;
        this.precipitationProbability = source.precipitationProbability;
        this.temperatureCurrent = source.temperatureCurrent;
        this.temperatureCompared = source.temperatureCompared;
        this.temperatureMin = source.temperatureMin;
        this.temperatureMax = source.temperatureMax;
        this.windSpeed = source.windSpeed;
        this.windSpeedWord = source.windSpeedWord;
        this.humidityCurrent = source.humidityCurrent;
        this.humidityCompared = source.humidityCompared;
    }
}
