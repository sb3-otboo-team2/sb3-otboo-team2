package org.ikuzo.otboo.domain.clothes.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ikuzo.otboo.global.base.BaseUpdatableEntity;

@Builder
@Entity
@Table(name = "clothes_attribute_defs")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesAttributeDef extends BaseUpdatableEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Builder.Default
    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttributeOption> options = new ArrayList<>();

    public void update(String newName, List<String> rawValues) {

        if (newName != null && !newName.isBlank() && !Objects.equals(this.name, newName)) {
            this.name = newName;
        }

        if (rawValues == null) return;

        List<String> normalizedValues = rawValues.stream()
            .map(v -> v == null ? null : v.trim())
            .filter(v -> v != null && !v.isEmpty())
            .distinct()
            .toList();

        Set<String> target = new LinkedHashSet<>(normalizedValues);

        Map<String, AttributeOption> currentByValue = this.options.stream()
            .collect(Collectors.toMap(AttributeOption::getValue, Function.identity(), (a,b) -> a));

        this.options.removeIf(opt -> !target.contains(opt.getValue()));

        for (String v : target) {
            if (!currentByValue.containsKey(v)) {
                this.options.add(AttributeOption.builder().value(v).definition(this).build());
            }
        }

    }
}
