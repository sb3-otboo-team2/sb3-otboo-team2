package org.ikuzo.otboo.domain.clothes.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDefDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefUpdateRequest;
import org.ikuzo.otboo.domain.clothes.entity.AttributeOption;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttributeDef;
import org.ikuzo.otboo.domain.clothes.enums.AttributeDefSortBy;
import org.ikuzo.otboo.domain.clothes.enums.AttributeDefSortDirection;
import org.ikuzo.otboo.domain.clothes.exception.AttributeDefinitionNotFoundException;
import org.ikuzo.otboo.domain.clothes.exception.DuplicatedAttributeNameException;
import org.ikuzo.otboo.domain.clothes.exception.MissingRequiredFieldException;
import org.ikuzo.otboo.domain.clothes.mapper.ClothesAttributeDefMapper;
import org.ikuzo.otboo.domain.clothes.repository.ClothesAttributeDefRepository;
import org.ikuzo.otboo.domain.clothes.service.ClothesAttributeDefService;
import org.ikuzo.otboo.global.event.message.ClothesAttributeDefCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesAttributeDefServiceImpl implements ClothesAttributeDefService {

    private final ClothesAttributeDefRepository clothesAttributeDefRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ClothesAttributeDefMapper mapper;

    @Transactional(readOnly = true)
    @Override
    public List<ClothesAttributeDefDto> getList(
        AttributeDefSortBy sortBy,
        AttributeDefSortDirection sortDirection,
        String keywordLike
    ) {
        log.info("[Service] 속성 목록 조회 시작 - sortBy: {}, sortDirection: {}, keywordLike: {}",
            sortBy, sortDirection, Objects.equals(normalizeKeyword(keywordLike), "") ? "공백" : keywordLike);

        String normalizeKeyword = normalizeKeyword(keywordLike);

        List<ClothesAttributeDef> data = clothesAttributeDefRepository.findAttributeDefWithCursor(
            sortBy, sortDirection, normalizeKeyword
        );

        log.info("[Service] 속성 목록 조회 완료 - sortBy: {}, sortDirection: {}, keywordLike: {}",
            sortBy, sortDirection, Objects.equals(normalizeKeyword(keywordLike), "") ? "공백" : keywordLike);

        return data.stream()
            .map(mapper::toDto)
            .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request) {

        String name = normalizeNameOrThrow(request.name());
        List<String> selectableValues = normalizeValuesOrThrow(request.selectableValues());

        log.info("[Service] 속성 등록 시작 - name: {}, selectableValues: {}", name, selectableValues);

        if (clothesAttributeDefRepository.existsByName(name)) {
            throw new DuplicatedAttributeNameException(name);
        }

        ClothesAttributeDef clothesAttributeDef = ClothesAttributeDef.builder()
            .name(name)
            .build();

        attachOptions(clothesAttributeDef, selectableValues);

        try {
            ClothesAttributeDef saved = clothesAttributeDefRepository.save(clothesAttributeDef);
            ClothesAttributeDefDto dto = mapper.toDto(saved);

            eventPublisher.publishEvent(
                new ClothesAttributeDefCreatedEvent(dto, Instant.now())
            );

            log.info("[Service] 속성 등록 완료 - id: {}, name: {}", saved.getId(), saved.getName());
            return dto;
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedAttributeNameException(name);
        }

    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public ClothesAttributeDefDto update(UUID definitionId,
        ClothesAttributeDefUpdateRequest request) {

        String newName = normalizeNameOrThrow(request.name());
        List<String> selectableValues = normalizeValuesOrThrow(request.selectableValues());

        log.info("[Service] 속성 수정 시작 - name: {}, selectableValues: {}", newName, selectableValues);

        ClothesAttributeDef def = clothesAttributeDefRepository.findById(definitionId)
            .orElseThrow(() -> new AttributeDefinitionNotFoundException(definitionId));

        if (!newName.equals(def.getName()) && clothesAttributeDefRepository.existsByName(newName)) {
            throw new DuplicatedAttributeNameException(newName);
        }

        def.update(newName, selectableValues);
        try {
            ClothesAttributeDef saved = clothesAttributeDefRepository.save(def);
            log.info("[Service] 속성 수정 완료 - id: {}, name: {}", saved.getId(), saved.getName());
            return mapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedAttributeNameException(newName);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public void delete(UUID definitionId) {

        log.info("[Service] 속성 삭제 시작 - definitionId: {}", definitionId);

        ClothesAttributeDef def = clothesAttributeDefRepository.findById(definitionId)
            .orElseThrow(() -> new AttributeDefinitionNotFoundException(definitionId));

        clothesAttributeDefRepository.delete(def);

        log.info("[Service] 속성 삭제 완료 - definitionId: {}", definitionId);
    }

    private String normalizeKeyword(String kw) {
        if (kw == null) {
            return null;
        }
        kw = kw.trim();
        return kw.isEmpty() ? null : kw;
    }

    private String normalizeNameOrThrow(String rawName) {
        String name = rawName == null ? null : rawName.trim();
        if (name == null || name.isEmpty()) {
            throw new MissingRequiredFieldException("name is null or blank");
        }
        return name;
    }

    private List<String> normalizeValuesOrThrow(List<String> rawValues) {
        if (rawValues == null) {
            throw new MissingRequiredFieldException("selectableValues is null");
        }
        Set<String> set = new LinkedHashSet<>();
        for (String v : rawValues) {
            if (v == null) {
                continue;
            }
            String t = v.trim();
            if (!t.isEmpty()) {
                set.add(t);
            }
        }
        if (set.isEmpty()) {
            throw new MissingRequiredFieldException(
                "selectableValues is empty after normalization");
        }
        return new ArrayList<>(set);
    }

    private void attachOptions(ClothesAttributeDef def, List<String> values) {
        values.forEach(v ->
            def.getOptions().add(AttributeOption.builder()
                .value(v)
                .definition(def)
                .build())
        );
    }
}
