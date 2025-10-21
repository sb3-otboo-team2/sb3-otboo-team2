package org.ikuzo.otboo.domain.clothes.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttributeDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesAttributeDefRepository extends JpaRepository<ClothesAttributeDef, UUID>,
    ClothesAttributeDefRepositoryCustom {

    boolean existsByName(String name);

    @Query("""
        select distinct d
        from ClothesAttributeDef d
        left join fetch d.options
        where lower(d.name) in :lowerNames
        """)
    List<ClothesAttributeDef> findAllByNameInIgnoreCase(
        @Param("lowerNames") Collection<String> lowerNames);
}
