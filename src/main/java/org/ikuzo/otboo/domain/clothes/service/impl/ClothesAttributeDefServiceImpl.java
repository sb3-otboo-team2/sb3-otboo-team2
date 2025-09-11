package org.ikuzo.otboo.domain.clothes.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDefDto;
import org.ikuzo.otboo.domain.clothes.entity.AttributeOption;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttributeDef;
import org.ikuzo.otboo.domain.clothes.exception.DuplicatedAttributeNameException;
import org.ikuzo.otboo.domain.clothes.mapper.ClothesAttributeDefMapper;
import org.ikuzo.otboo.domain.clothes.repository.ClothesAttributeDefRepository;
import org.ikuzo.otboo.domain.clothes.service.ClothesAttributeDefService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesAttributeDefServiceImpl implements ClothesAttributeDefService {

    private final ClothesAttributeDefRepository clothesAttributeDefRepository;
    private final ClothesAttributeDefMapper mapper;

    @Transactional
    @Override
    public ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request) {

        String name = request.name();
        List<String> selectableValues = request.selectableValues();

        log.info("[Service] 속성 등록 시작 - name: {}, selectableValues: {}", name, selectableValues);

        if (clothesAttributeDefRepository.existsByName(name)) {
            throw new DuplicatedAttributeNameException(name);
        }

        ClothesAttributeDef clothesAttributeDef = ClothesAttributeDef.builder()
            .name(name)
            .build();

        if (selectableValues != null) {
            selectableValues.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> AttributeOption.builder()
                    .value(value)
                    .definition(clothesAttributeDef)
                    .build())
                .forEach(clothesAttributeDef.getOptions()::add);
        }

        ClothesAttributeDef savedClothesAttributeDef;

        try {
            savedClothesAttributeDef = clothesAttributeDefRepository.save(clothesAttributeDef);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedAttributeNameException(name);
        }

        log.info("[Service] 속성 등록 시작 - name: {}, selectableValues: {}", name, selectableValues);

        return mapper.toDto(savedClothesAttributeDef);
    }
}
