package org.ikuzo.otboo.domain.clothes.service.impl;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDto;
import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesUpdateRequest;
import org.ikuzo.otboo.domain.clothes.entity.AttributeOption;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttribute;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttributeDef;
import org.ikuzo.otboo.domain.clothes.exception.AttributeDefinitionNotFoundException;
import org.ikuzo.otboo.domain.clothes.exception.ClothesNotFoundException;
import org.ikuzo.otboo.domain.clothes.exception.InvalidAttributeOptionException;
import org.ikuzo.otboo.domain.clothes.exception.MissingRequiredFieldException;
import org.ikuzo.otboo.domain.clothes.mapper.ClothesMapper;
import org.ikuzo.otboo.domain.clothes.repository.ClothesAttributeDefRepository;
import org.ikuzo.otboo.domain.clothes.repository.ClothesRepository;
import org.ikuzo.otboo.domain.clothes.service.ClothesService;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.global.util.ImageSwapHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesServiceImpl implements ClothesService {

    private final UserRepository userRepository;
    private final ClothesRepository clothesRepository;
    private final ClothesAttributeDefRepository clothesAttributeDefRepository;
    private final ClothesMapper clothesMapper;
    private final ImageSwapHelper imageSwapHelper;

    @Transactional
    @Override
    public ClothesDto create(ClothesCreateRequest request, MultipartFile image) {
        log.info("[Service] 의상 등록 시작 - ownerId: {}, name: {}", request.ownerId(), request.name());

        validateClothesCreateRequest(request);

        User owner = userRepository.findById(request.ownerId())
            .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다"));

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = imageSwapHelper.swapImageSafely(image, null, owner.getId());
        }

        Clothes clothes = Clothes.builder()
            .name(request.name())
            .type(request.type())
            .owner(owner)
            .imageUrl(imageUrl)
            .build();

        attachAttributes(clothes, request.attributes());

        Clothes saved = clothesRepository.save(clothes);

        log.info("[Service] 의상 등록 완료 - ownerId: {}, name: {}",
            saved.getOwner().getId(), saved.getName());

        return clothesMapper.toDto(saved);

    }

    @Transactional
    @Override
    public ClothesDto update(UUID clothesId, ClothesUpdateRequest request, MultipartFile image) {

        log.info("[Service] 의상 수정 시작 - clothesId: {}, name: {}", clothesId, request.name());

        validateUpdateRequest(clothesId, request);

        Clothes clothes = clothesRepository.findById(clothesId)
            .orElseThrow(() -> new ClothesNotFoundException(clothesId));

        clothes.updateNameAndType(request.name(), request.type());

        String newImageUrl = imageSwapHelper.swapImageSafely(
            image, clothes.getImageUrl(), clothes.getOwner().getId());

        if (newImageUrl != null) {
            clothes.updateImageUrl(newImageUrl);
        }

        if (request.attributes() != null) {
            replaceAttribute(clothes, request.attributes());
        }

        Clothes savedClothes = clothesRepository.save(clothes);

        log.info("[Service] 의상 수정 완료 - clothesId: {}, name: {}",
            savedClothes.getId(), savedClothes.getName());

        return clothesMapper.toDto(savedClothes);
    }

    @Transactional
    @Override
    public void delete(UUID clothesId) {

        log.info("[Service] 의상 삭제 시작 - clothesId: {}", clothesId);

        Clothes clothes = clothesRepository.findById(clothesId)
                .orElseThrow(() -> new ClothesNotFoundException(clothesId));

        String oldImageUrl = clothes.getImageUrl();

        clothesRepository.delete(clothes);

        imageSwapHelper.deleteAfterCommit(oldImageUrl, "의상 삭제");

        log.info("[Service] 의상 삭제 완료 - clothesId: {}", clothesId);

    }

    private void validateClothesCreateRequest (ClothesCreateRequest request) {
        if (request.ownerId() == null) {
            throw new MissingRequiredFieldException("ownerId is null");
        }
        if (request.name() == null) {
            throw new MissingRequiredFieldException("name is null");
        }
        if (request.type() == null) {
            throw new MissingRequiredFieldException("type is null");
        }
    }

    private void validateUpdateRequest(UUID clothesId, ClothesUpdateRequest request) {
        if (clothesId == null) {
            throw new MissingRequiredFieldException("clothesId is null");
        }
        if (request.name() == null) {
            throw new MissingRequiredFieldException("name is null");
        }
        if (request.type() == null) {
            throw new MissingRequiredFieldException("type is null");
        }
    }

    private void attachAttributes(Clothes clothes, List<ClothesAttributeDto> dtos) {
        if (dtos == null) {
            return;
        }
        for (ClothesAttributeDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            clothes.getAttributes().add(toClothesAttribute(clothes, dto));
        }
    }

    private ClothesAttribute toClothesAttribute(Clothes clothes, ClothesAttributeDto dto) {
        UUID defId = dto.definitionId();
        String value = dto.value() == null ? null : dto.value().trim();

        ClothesAttributeDef def = clothesAttributeDefRepository.findById(defId)
            .orElseThrow(() -> new AttributeDefinitionNotFoundException(defId));

        if (value == null || value.isEmpty()) {
            throw new MissingRequiredFieldException(
                "속성 값은 비어 있을 수 없습니다. definition=" + def.getName()
            );
        }

        List<String> selectable = getSelectableValues(def);
        if (!selectable.isEmpty() && !selectable.contains(value)) {
            throw new InvalidAttributeOptionException(
                "해당 속성에서 선택 불가한 옵션 값 입니다. definition=" + def.getName()
                    + ", 입력값=" + value + ", 허용=" + selectable);
        }

        return ClothesAttribute.builder()
            .clothes(clothes)
            .definition(def)
            .optionValue(value)
            .build();
    }

    private List<String> getSelectableValues(ClothesAttributeDef def) {
        List<AttributeOption> options = def.getOptions();
        return options == null ? List.of()
            : options.stream().map(AttributeOption::getValue).toList();
    }

    private void replaceAttribute(Clothes clothes, List<ClothesAttributeDto> dtos) {

        clothes.getAttributes().clear();

        for (ClothesAttributeDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            clothes.getAttributes().add(toClothesAttribute(clothes, dto));
        }
    }
}
