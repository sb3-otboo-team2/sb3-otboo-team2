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
import lombok.RequiredArgsConstructor;
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
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Primary
@Slf4j
@Component
@RequiredArgsConstructor
public class LLMRecommendationEngine implements RecommendationEngine {

    private static final double OPENAI_TEMPERATURE = 0.3;

    private final ClothesRepository clothesRepository;
    private final OpenAiChatClient chatClient;
    private final OpenAiProps openAiProps;
    private final ObjectMapper objectMapper;

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
            return fallback(clothes);
        }
        return result;
    }

    private Mono<String> callOpenAi(String systemPrompt, String userPrompt) {
        return chatClient.chatJson(openAiProps.model(), systemPrompt, userPrompt, OPENAI_TEMPERATURE);
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
            JsonNode summary = root.path("summary");
            log.info("의상 선택 이유: {}", summary.toString());

            List<RecommendResponse.Pick> out = new ArrayList<>();
            if (picks.isArray()) {
                for (JsonNode p : picks) {
                    try {
                        UUID id = UUID.fromString(p.path("id").asText());
                        double score = clamp0to1(p.path("score").asDouble(0.5));
                        out.add(new RecommendResponse.Pick(id, score));
                    } catch (Exception ignore) {}
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

    private double clamp0to1(Double d) {
        double v = d == null ? 0.5 : d;
        if (v < 0) {
            return 0;
        }
        if (v > 1) {
            return 1;
        }
        return v;
    }

    private List<Clothes> fallback(List<Clothes> wardrobe) {
        int n = Math.min(3, wardrobe.size());
        return wardrobe.subList(0, n);
    }
}
