package org.ikuzo.otboo.domain.clothes.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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

    @Column(name = "name", nullable = false)
    private String name;

    @Builder.Default
    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttributeOption> options = new ArrayList<>();
}
