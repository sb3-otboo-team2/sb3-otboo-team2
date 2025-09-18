package org.ikuzo.otboo.domain.weather.service;


import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.weather.entity.Weather;

public interface WeatherAlertService {

    void checkAndNotify(User user, Weather latest);
}
