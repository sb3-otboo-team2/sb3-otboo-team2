package org.ikuzo.otboo.domain.clothes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ikuzo.otboo.domain.clothes.exception.InvalidAttributeOptionException;
import org.ikuzo.otboo.domain.clothes.exception.MissingRequiredFieldException;
import org.ikuzo.otboo.global.base.BaseUpdatableEntity;

@Builder
@Entity
@Table(
    name = "clothes_attributes",
    indexes = {
        @Index(name = "idx_clothes_attr_clothes", columnList = "clothes_id"),
        @Index(name = "idx_clothes_attr_def", columnList = "definition_id")
    }
)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesAttribute extends BaseUpdatableEntity {

    @Column(name = "option_value", nullable = false)
    private String optionValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothes clothes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definition_id", nullable = false)
    private ClothesAttributeDef definition;

    public void updateOptionValue(String newValue) {
        if (newValue == null || newValue.isBlank()) {
            throw new MissingRequiredFieldException("optionValue 비어있음");
        }
        // 선택지 검증(있을 때만)
        if (definition != null && definition.getOptions() != null && !definition.getOptions()
            .isEmpty()) {
            boolean allowed = definition.getOptions().stream()
                .map(AttributeOption::getValue)
                .anyMatch(newValue::equals);
            if (!allowed) {
                throw new InvalidAttributeOptionException(
                    "해당 속성에서 선택 불가한 옵션 값 입니다. definition=" + definition.getName() + ", 입력값=" + newValue);
            }
        }
        this.optionValue = newValue;
    }

}
