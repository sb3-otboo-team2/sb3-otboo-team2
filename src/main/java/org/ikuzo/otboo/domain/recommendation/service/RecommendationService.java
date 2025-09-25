package org.ikuzo.otboo.domain.recommendation.service;

import java.util.UUID;
import org.ikuzo.otboo.domain.recommendation.dto.RecommendationDto;

public interface RecommendationService {

    RecommendationDto create(UUID weatherId);
}
