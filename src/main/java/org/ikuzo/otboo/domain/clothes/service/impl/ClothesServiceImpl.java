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
import org.ikuzo.otboo.global.util.S3ImageStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesServiceImpl implements ClothesService {

    private final UserRepository userRepository;
    private final S3ImageStorage s3ImageStorage;
    private final ClothesRepository clothesRepository;
    private final ClothesAttributeDefRepository clothesAttributeDefRepository;
    private final ClothesMapper clothesMapper;

    @Transactional
    @Override
    public ClothesDto create(ClothesCreateRequest request, MultipartFile image) {
        log.info("[Service] 의상 등록 시작 - ownerId: {}, name: {}", request.ownerId(), request.name());

        validateClothesCreateRequest(request);

        User owner = userRepository.findById(request.ownerId())
            .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다"));

        String imageUrl = null;
        try {
            imageUrl = uploadImageIfPresent(image, owner.getId());

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

        } catch (RuntimeException e) {
            cleanupUploadedImageQuietly(imageUrl);
            throw e;
        }
    }

    @Transactional
    @Override
    public ClothesDto update(UUID clothesId, ClothesUpdateRequest request, MultipartFile image) {

        log.info("[Service] 의상 수정 시작 - clothesId: {}, name: {}", clothesId, request.name());

        validateUpdateRequest(clothesId, request);

        Clothes clothes = clothesRepository.findById(clothesId)
            .orElseThrow(() -> new ClothesNotFoundException(clothesId));

        clothes.updateNameAndType(request.name(), request.type());

        String oldImageUrl = clothes.getImageUrl();
        String newImageUrl = uploadImageIfPresent(image, clothes.getOwner().getId());

        if (newImageUrl != null) {
            clothes.updateImageUrl(newImageUrl);
            cleanupUploadedImageQuietly(oldImageUrl);
        }

        if (request.attributes() != null) {
            replaceAttribute(clothes, request.attributes());
        }

        Clothes savedClothes = clothesRepository.save(clothes);

        log.info("[Service] 의상 수정 완료 - clothesId: {}, name: {}",
            savedClothes.getId(), savedClothes.getName());

        return clothesMapper.toDto(savedClothes);
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


    private String uploadImageIfPresent(MultipartFile image, UUID ownerId) {
        if (image == null || image.isEmpty()) {
            return null;
        }
        validateImage(image);
        String folder = "clothes/" + ownerId + "/";
        return s3ImageStorage.uploadImage(image, folder);
    }

    private void validateImage(MultipartFile image) {
        long maxBytes = 10L * 1024 * 1024;
        if (image.getSize() > maxBytes) {
            throw new IllegalArgumentException("이미지 크기 제한 초과(최대 10MB)");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
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

    private void cleanupUploadedImageQuietly(String imageUrl) {
        if (imageUrl == null) {
            return;
        }
        try {
            s3ImageStorage.deleteImage(imageUrl);
        } catch (Exception ex) {
            log.error("DB 롤백 중 업로드 이미지 정리 실패: {}", imageUrl, ex);
        }
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
