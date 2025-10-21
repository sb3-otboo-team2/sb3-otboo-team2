package org.ikuzo.otboo.domain.recommendation.service.engine;

import java.util.List;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.weather.entity.Weather;

public interface RecommendationEngine {

    List<Clothes> recommend(User owner, Weather weather);

}
