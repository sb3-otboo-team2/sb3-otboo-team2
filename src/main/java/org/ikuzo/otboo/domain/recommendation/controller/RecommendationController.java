package org.ikuzo.otboo.domain.recommendation.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.recommendation.controller.api.RecommendationApi;
import org.ikuzo.otboo.domain.recommendation.dto.OotdDto;
import org.ikuzo.otboo.domain.recommendation.dto.RecommendationDto;
import org.ikuzo.otboo.domain.recommendation.service.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController implements RecommendationApi {

    private final RecommendationService recommendationService;

    @GetMapping
    @Override
    public ResponseEntity<RecommendationDto> create(
        @RequestParam UUID weatherId
    ) {
        log.info("[Controller] 추천 조회 요청 - weatherId: {}", weatherId);

        RecommendationDto response = recommendationService.create(weatherId);

        List<String> clothesName = response.clothes().stream()
                .map(OotdDto::name)
                    .toList();

        log.info("[Controller] 추천 조회 완료 - clothes: {}", clothesName);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{weatherId}")
    public ResponseEntity<Double> test(
        @PathVariable UUID weatherId
    ) {
        log.info("[Controller] 체감온도 계산 요청 - weatherId: {}", weatherId);

        double response = recommendationService.test(weatherId);

        log.info("[Controller] 체감온도 계산 완료 - 체감 온도: {}", response);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
