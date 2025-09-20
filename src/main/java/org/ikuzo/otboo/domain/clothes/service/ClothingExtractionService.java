package org.ikuzo.otboo.domain.clothes.service;

import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import reactor.core.publisher.Mono;

public interface ClothingExtractionService {
    Mono<ClothesDto> extractFromUrlReactive(String url);

}
