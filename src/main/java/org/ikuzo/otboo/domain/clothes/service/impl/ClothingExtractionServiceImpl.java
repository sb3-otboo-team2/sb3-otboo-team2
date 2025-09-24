package org.ikuzo.otboo.domain.clothes.service.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeWithDefDto;
import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import org.ikuzo.otboo.domain.clothes.entity.AttributeOption;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttributeDef;
import org.ikuzo.otboo.domain.clothes.enums.ClothesType;
import org.ikuzo.otboo.domain.clothes.extractions.OpenAiHtmlExtractor;
import org.ikuzo.otboo.domain.clothes.dto.ExtractClothes;
import org.ikuzo.otboo.domain.clothes.parser.HtmlParserResolver;
import org.ikuzo.otboo.domain.clothes.repository.ClothesAttributeDefRepository;
import org.ikuzo.otboo.domain.clothes.service.ClothingExtractionService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothingExtractionServiceImpl implements ClothingExtractionService {

    private final HtmlParserResolver htmlParserResolver;
    private final OpenAiHtmlExtractor llmExtractor;
    private final ClothesAttributeDefRepository defRepo;

    @Override
    public Mono<ClothesDto> extractFromUrlReactive(String url) {

        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("잘못된 URL 형식: " + url, e));
        }

        return Mono.fromCallable(() -> htmlParserResolver.parse(uri))
            .onErrorMap(e -> new RuntimeException("HTML 파싱 실패: " + e.getMessage(), e))
            .flatMap(parsed -> llmExtractor.extract(uri, parsed, loadAllowedDefs()))
            .map(this::toDto)
            .map(this::enrichAttributesByDefinitions)
            .doOnSubscribe(s -> log.info("[Service] LLM Extractor 시작, URL: {}", url))
            .doOnSuccess(dto -> log.info("[Service] LLM Extractor 완료, URL: {}", url));
    }

    private Map<String, List<String>> loadAllowedDefs() {
        return defRepo.findAll().stream().collect(
            Collectors.toMap(
                ClothesAttributeDef::getName,
                d -> d.getOptions().stream().map(AttributeOption::getValue).toList(),
                (a, b) -> a,
                java.util.LinkedHashMap::new
            )
        );
    }

    private ClothesDto toDto(ExtractClothes ex) {
        List<ClothesAttributeWithDefDto> attrs = ex.attributes() == null ? List.of() :
            ex.attributes().stream()
                .map(a -> new ClothesAttributeWithDefDto(
                    null,
                    a.definitionName(),
                    List.of(),
                    a.value()
                )).toList();

        return new ClothesDto(
            null,
            null,
            ex.name(),
            ex.imageUrl(),
            safeType(ex.type()),
            attrs
        );
    }

    private ClothesType safeType(String raw) {
        if (raw == null) {
            return ClothesType.ETC;
        }
        try {
            return ClothesType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return ClothesType.ETC;
        }
    }

    protected ClothesDto enrichAttributesByDefinitions(ClothesDto dto) {
        List<ClothesAttributeWithDefDto> attrs = dto.attributes();
        if (attrs == null || attrs.isEmpty()) return dto;

        List<String> defNamesLower = collectDefNamesLower(attrs);
        Map<String, ClothesAttributeDef> defsMap = loadDefsMap(defNamesLower);
        List<ClothesAttributeWithDefDto> mapped = mapAttributesWithDefinitions(attrs, defsMap);

        return new ClothesDto(dto.id(), dto.ownerId(), dto.name(), dto.imageUrl(), dto.type(), mapped);
    }

    private static List<String> collectDefNamesLower(List<ClothesAttributeWithDefDto> attrs) {
        return attrs.stream()
            .map(ClothesAttributeWithDefDto::definitionName)
            .filter(Objects::nonNull)
            .map(s -> s.toLowerCase(Locale.ROOT))
            .distinct()
            .toList();
    }

    private Map<String, ClothesAttributeDef> loadDefsMap(List<String> defNamesLower) {
        return defRepo.findAllByNameInIgnoreCase(defNamesLower).stream()
            .collect(Collectors.toMap(
                d -> d.getName().toLowerCase(Locale.ROOT),
                Function.identity(),
                (a, b) -> a,
                LinkedHashMap::new
            ));
    }

    private static List<ClothesAttributeWithDefDto> mapAttributesWithDefinitions(
        List<ClothesAttributeWithDefDto> attrs,
        Map<String, ClothesAttributeDef> defsMap
    ) {
        List<ClothesAttributeWithDefDto> mapped = new ArrayList<>(attrs.size());
        for (ClothesAttributeWithDefDto a : attrs) {
            String key = normOrNull(a.definitionName());
            ClothesAttributeDef def = key == null ? null : defsMap.get(key);

            if (def != null) {
                List<String> values = def.getOptions().stream()
                    .map(AttributeOption::getValue)
                    .toList();

                mapped.add(new ClothesAttributeWithDefDto(
                    def.getId(),
                    def.getName(),
                    values,
                    a.value()
                ));
            } else {
                mapped.add(a);
            }
        }
        return mapped;
    }

    private static String normOrNull(String s) {
        return (s == null) ? null : s.toLowerCase(Locale.ROOT);
    }
}
