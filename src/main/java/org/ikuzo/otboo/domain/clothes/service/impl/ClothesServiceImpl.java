package org.ikuzo.otboo.domain.clothes.service.impl;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDto;
import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesCreateRequest;
import org.ikuzo.otboo.domain.clothes.entity.AttributeOption;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttribute;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttributeDef;
import org.ikuzo.otboo.domain.clothes.entity.ClothesType;
import org.ikuzo.otboo.domain.clothes.exception.AttributeDefinitionNotFoundException;
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
    private final ClothesAttributeDefRepository attributeDefRepository;
    private final ClothesRepository clothesRepository;
    private final ClothesMapper clothesMapper;

    @Transactional
    @Override
    public ClothesDto create(ClothesCreateRequest request, MultipartFile image) {

        UUID ownerId = request.ownerId();
        String name = request.name();
        ClothesType type = request.type();

        log.info("[Service] 의상 등록 시작 - ownerId: {}, name: {}", ownerId, name);

        if (ownerId == null || name == null || type == null) {
            throw new IllegalArgumentException("ownerId or name or type is null");
        }

        User owner = userRepository.findById(ownerId).orElseThrow(
            () -> new RuntimeException("존재하지 않는 사용자입니다")
        );

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            String folderPath = "clothes/" + ownerId + "/";
            imageUrl = s3ImageStorage.uploadImage(image, folderPath);
        }

        try {
            Clothes clothes = Clothes.builder()
                .name(name)
                .type(type)
                .owner(owner)
                .imageUrl(imageUrl)
                .build();

            if (request.attributes() != null) {
                for (ClothesAttributeDto attrRequest : request.attributes()) {
                    UUID definitionId = attrRequest.definitionId();
                    String value = attrRequest.value();

                    ClothesAttributeDef definition = attributeDefRepository.findById(definitionId)
                        .orElseThrow(
                            () -> new AttributeDefinitionNotFoundException(ownerId)
                        );

                    if (value == null || value.isBlank()) {
                        throw new IllegalArgumentException(
                            "속성 값은 비어 있을 수 없습니다. definition=" + definition.getName()
                        );
                    }

                    List<String> selectable = definition.getOptions().stream()
                        .map(AttributeOption::getValue)
                        .toList();

                    if (!selectable.isEmpty() && !selectable.contains(value)) {
                        throw new IllegalArgumentException(
                            "해당 속성에서 선택 불가한 옵션 값 입니다. definition=" + definition.getName()
                                + ", 입력값=" + value
                                + ", 허용=" + selectable);
                    }

                    ClothesAttribute attr = ClothesAttribute.builder()
                        .clothes(clothes)
                        .definition(definition)
                        .optionValue(value)
                        .build();

                    clothes.getAttributes().add(attr);
                }
            }

            Clothes savedClothes = clothesRepository.save(clothes);

            log.info("[Service] 의상 등록 완료 - ownerId: {}, name: {}",
                savedClothes.getOwner().getId(), savedClothes.getName());

            return clothesMapper.toDto(savedClothes);

        } catch (RuntimeException e) {
            if (imageUrl != null) {
                try {
                    s3ImageStorage.deleteImage(imageUrl);
                } catch (Exception cleanupException) {
                    log.error("DB 롤백 중 업로드 이미지 정리 실패: {}", imageUrl, cleanupException);
                }
            }
            throw e;
        }
    }
}
