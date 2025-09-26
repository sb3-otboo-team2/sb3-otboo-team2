package org.ikuzo.otboo.domain.recommendation.service.engine.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.config.OpenAiProps;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttributeDef;
import org.ikuzo.otboo.domain.clothes.extractions.OpenAiChatClient;
import org.ikuzo.otboo.domain.clothes.repository.ClothesRepository;
import org.ikuzo.otboo.domain.recommendation.dto.request.RecommendRequest;
import org.ikuzo.otboo.domain.recommendation.dto.response.RecommendResponse;
import org.ikuzo.otboo.domain.recommendation.service.engine.RecommendationEngine;
import org.ikuzo.otboo.domain.recommendation.util.OpenAiPromptTemplates;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Primary
@Slf4j
@Component
public class LLMRecommendationEngine implements RecommendationEngine {

    private static final double OPENAI_TEMPERATURE = 0.3;

    private final ClothesRepository clothesRepository;
    private final OpenAiChatClient chatClient;
    private final OpenAiProps openAiProps;
    private final ObjectMapper objectMapper;
    private final RecommendationEngine randomEngine;

    public LLMRecommendationEngine(
        ClothesRepository clothesRepository,
        OpenAiChatClient chatClient,
        OpenAiProps openAiProps,
        ObjectMapper objectMapper,
        @Qualifier("randomRecommendationEngine") RecommendationEngine randomEngine
    ) {
        this.clothesRepository = clothesRepository;
        this.chatClient = chatClient;
        this.openAiProps = openAiProps;
        this.objectMapper = objectMapper;
        this.randomEngine = randomEngine;
    }

    @Override
    public List<Clothes> recommend(User owner, Weather weather) {

        log.info("[OpenAiRecommendationEngine] recommendation start");

        List<Clothes> clothes = clothesRepository.findByOwnerId(owner.getId());
        if (clothes == null || clothes.isEmpty()) {
            log.info("[LLM-REC] empty wardrobe for user={}", owner.getId());
            return List.of();
        }

        RecommendRequest request = toRequest(owner, weather, clothes);

        String systemPrompt = OpenAiPromptTemplates.systemPrompt();
        String userPrompt = OpenAiPromptTemplates.userPrompt(request);

        String content = Mono.defer(() -> callOpenAi(systemPrompt, userPrompt))
            .subscribeOn(Schedulers.boundedElastic())
            .timeout(Duration.ofSeconds(12))
            .onErrorResume(e -> {
                log.warn("[LLM-REC] OpenAI call failed: {}", e.toString());
                return Mono.just("{}");
            })
            .block();

        RecommendResponse res = parseResponse(content);

        List<Clothes> result = toClothesSorted(res, clothes);

        if (result.isEmpty()) {
            return fallback(owner, weather, clothes);
        }
        return result;
    }

    private Mono<String> callOpenAi(String systemPrompt, String userPrompt) {
        return chatClient.chatJson(openAiProps.model(), systemPrompt, userPrompt,
            OPENAI_TEMPERATURE);
    }

    private RecommendRequest toRequest(User owner, Weather weather, List<Clothes> clothes) {
        return new RecommendRequest(
            owner.getId(),
            owner.getGender() == null ? null : owner.getGender().name(),
            owner.getTemperatureSensitivity(),
            normalize(weather.getTemperatureCurrent()),
            normalize(weather.getHumidityCurrent()),
            weather.getSkyStatus() == null ? null : weather.getSkyStatus(),
            weather.getPrecipitationType() == null ? null : weather.getPrecipitationType(),
            weather.getWindSpeedWord() == null ? null : weather.getWindSpeedWord(),
            clothes.stream()
                .limit(100)
                .map(this::toClothesItem)
                .toList()
        );
    }

    private RecommendRequest.WardrobeItem toClothesItem(Clothes c) {
        List<String> attrs = new ArrayList<>();
        if (c.getAttributes() != null) {
            c.getAttributes().stream()
                .limit(6)
                .forEach(a -> {
                    try {
                        ClothesAttributeDef def = a.getDefinition();
                        String defName =
                            (def != null && def.getName() != null) ? def.getName() : "속성";
                        String value = a.getOptionValue() == null ? "" : a.getOptionValue();
                        attrs.add(defName + ":" + value);
                    } catch (Exception ignore) {
                    }
                });
        }
        return new RecommendRequest.WardrobeItem(
            c.getId(),
            c.getName(),
            c.getType() == null ? "ETC" : c.getType().name(),
            attrs
        );
    }

    private RecommendResponse parseResponse(String content) {
        if (content == null || content.isBlank()) {
            return new RecommendResponse(List.of());
        }
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode picks = root.path("picks");

            List<RecommendResponse.Pick> out = new ArrayList<>();
            if (picks.isArray()) {
                for (JsonNode p : picks) {
                    try {
                        UUID id = UUID.fromString(p.path("id").asText());
                        double score = p.path("score").asDouble(50);
                        double clampScore = clamp(score);
                        out.add(new RecommendResponse.Pick(id, clampScore));
                    } catch (Exception ignore) {
                    }
                }
            }
            return new RecommendResponse(out);
        } catch (Exception e) {
            log.warn("[LLM-REC] parse fail: {}", e.toString());
            return new RecommendResponse(List.of());
        }
    }

    // == Clothes 재조회 + 정렬 ==
    private List<Clothes> toClothesSorted(RecommendResponse res, List<Clothes> wardrobe) {
        if (res == null || res.picks() == null || res.picks().isEmpty()) {
            return List.of();
        }

        Map<UUID, Double> scoreMap = res.picks().stream()
            .collect(Collectors.toMap(RecommendResponse.Pick::id, p -> normalize(p.score())));

        // 후보 id만 추림
        Set<UUID> idSet = scoreMap.keySet();

        // 점수 내림차순 정렬
        return wardrobe.stream()
            .filter(c -> idSet.contains(c.getId())).sorted(Comparator.comparingDouble(
                c -> -scoreMap.getOrDefault(c.getId(), 0.0)))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private double normalize(Double d) {
        return d == null ? 0.0 : d;
    }

    private double clamp(double score) {
        return Math.max(0.0, Math.min(100.0, score)) / 100.0;
    }

    private List<Clothes> fallback(User owner, Weather weather, List<Clothes> wardrobe) {
        try {
            return randomEngine.recommend(owner, weather);
        } catch (Exception e) {
            int n = Math.min(3, wardrobe.size());
            return wardrobe.subList(0, n);
        }
    }
}
