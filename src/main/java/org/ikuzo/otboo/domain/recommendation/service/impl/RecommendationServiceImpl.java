package org.ikuzo.otboo.domain.recommendation.service.impl;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.domain.clothes.mapper.ClothesMapper;
import org.ikuzo.otboo.domain.recommendation.dto.RecommendationDto;
import org.ikuzo.otboo.domain.recommendation.entity.Recommend;
import org.ikuzo.otboo.domain.recommendation.entity.RecommendationClothes;
import org.ikuzo.otboo.domain.recommendation.repository.RecommendRepository;
import org.ikuzo.otboo.domain.recommendation.repository.RecommendationClothesRepository;
import org.ikuzo.otboo.domain.recommendation.service.engine.RecommendationEngine;
import org.ikuzo.otboo.domain.recommendation.service.RecommendationService;
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

    private final RecommendRepository recommendRepository;
    private final RecommendationClothesRepository recommendationClothesRepository;
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

        Recommend recommend = persistRecommend(owner, weather);
        persistLinks(recommend, pickedClothes);

        List<ClothesDto> clothesDtos = pickedClothes.stream()
            .map(clothesMapper::toDto)
            .toList();

        log.info("[Service] 의상 추천 완료 - clothes: {}", clothesDtos);

        return toResponse(weather.getId(), owner.getId(), clothesDtos);
    }

    private Recommend persistRecommend(User owner, Weather weather) {
        Recommend recommend = Recommend.builder()
            .user(owner)
            .weather(weather)
            .build();
        return recommendRepository.save(recommend);
    }

    private void persistLinks(Recommend recommend, List<Clothes> pickedClothes) {
        List<RecommendationClothes> links = pickedClothes.stream()
            .map(c -> RecommendationClothes.builder()
                .recommend(recommend)
                .clothes(c)
                .build())
            .toList();
        recommendationClothesRepository.saveAll(links);
    }

    private RecommendationDto toResponse(UUID weatherId, UUID ownerId, List<ClothesDto> clothes) {
        return RecommendationDto.builder()
            .weatherId(weatherId)
            .userId(ownerId)
            .clothes(clothes)
            .build();
    }
}
