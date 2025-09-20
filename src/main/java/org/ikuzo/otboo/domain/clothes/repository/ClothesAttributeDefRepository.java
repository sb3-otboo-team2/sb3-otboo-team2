package org.ikuzo.otboo.domain.clothes.repository;

import java.util.Optional;
import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttributeDef;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesAttributeDefRepository extends JpaRepository<ClothesAttributeDef, UUID>,
    ClothesAttributeDefRepositoryCustom {

    boolean existsByName(String name);

    @EntityGraph(attributePaths = "options")
    Optional<ClothesAttributeDef> findByNameIgnoreCase(String name);

}
