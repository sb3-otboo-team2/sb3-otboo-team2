package org.ikuzo.otboo.domain.recommendation.service.engine.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.domain.clothes.enums.ClothesType;
import org.ikuzo.otboo.domain.clothes.repository.ClothesRepository;
import org.ikuzo.otboo.domain.recommendation.service.engine.RecommendationEngine;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RandomRecommendationEngine implements RecommendationEngine {

    private static final double OUTER_THRESHOLD_C = 25.0;
    private final ClothesRepository clothesRepository;

    @Override
    public List<Clothes> recommend(User owner, Weather weather) {
        UUID ownerId = owner.getId();

        boolean includeOuter = needsOuter(weather);

        return pickRandomClothesSet(ownerId, includeOuter);
    }

    private boolean needsOuter(Weather weather) {
        Double current = null;
        Object raw = weather.getTemperatureCurrent();
        if (raw instanceof Number num) {
            current = num.doubleValue();
        }
        return current != null && current <= OUTER_THRESHOLD_C;
    }

    private List<Clothes> pickRandomClothesSet(UUID ownerId, boolean includeOuter) {
        Clothes top    = pickRandom(ownerId, ClothesType.TOP);
        Clothes bottom = pickRandom(ownerId, ClothesType.BOTTOM);
        Clothes shoes  = pickRandom(ownerId, ClothesType.SHOES);
        Clothes accessory = pickRandom(ownerId, ClothesType.ACCESSORY);
        Clothes outer  = includeOuter ? pickRandom(ownerId, ClothesType.OUTER) : null;

        return Stream.of(outer,top, bottom, shoes, accessory)
            .filter(Objects::nonNull)
            .toList();
    }

    private Clothes pickRandom(UUID ownerId, ClothesType type) {
        return clothesRepository.pickRandomClothes(ownerId, type.name()).orElse(null);
    }
}

