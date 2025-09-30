package org.ikuzo.otboo.domain.recommendation.service.impl;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.domain.clothes.mapper.ClothesMapper;
import org.ikuzo.otboo.domain.recommendation.dto.OotdDto;
import org.ikuzo.otboo.domain.recommendation.dto.RecommendationDto;
import org.ikuzo.otboo.domain.recommendation.service.RecommendationService;
import org.ikuzo.otboo.domain.recommendation.service.engine.RecommendationEngine;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.ikuzo.otboo.domain.weather.exception.WeatherNotFoundException;
import org.ikuzo.otboo.domain.weather.repository.WeatherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationEngine recommendationEngine;
    private final WeatherRepository weatherRepository;
    private final ClothesMapper clothesMapper;

    @Transactional
    @Override
    public RecommendationDto create(UUID weatherId) {
        log.info("[Service] 의상 추천 시작 - weatherId: {}", weatherId);

        Weather weather = weatherRepository.findById(weatherId)
            .orElseThrow(WeatherNotFoundException::new);
        User owner = weather.getUser();

        List<Clothes> pickedClothes = recommendationEngine.recommend(owner, weather);

        List<OotdDto> clothesDtos = pickedClothes.stream()
                .map(clothesMapper::toOotdDto)
                    .toList();

        log.info("[Service] 의상 추천 완료 - clothes: {}", clothesDtos);

        return toResponse(weather.getId(), owner.getId(), clothesDtos);
    }

    private RecommendationDto toResponse(UUID weatherId, UUID ownerId, List<OotdDto> clothes) {
        return RecommendationDto.builder()
            .weatherId(weatherId)
            .userId(ownerId)
            .clothes(clothes)
            .build();
    }
}
