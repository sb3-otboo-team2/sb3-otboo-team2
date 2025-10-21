package org.ikuzo.otboo.domain.clothes.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesRepository extends JpaRepository<Clothes, UUID>, ClothesRepositoryCustom {

    @Query(value = """
            SELECT c.*
            FROM clothes c
            WHERE c.owner_id = :ownerId
              AND c.type = :type
            ORDER BY random()
            LIMIT 1
        """, nativeQuery = true)
    Optional<Clothes> pickRandomClothes(@Param("ownerId") UUID ownerId,
        @Param("type") String type);

    List<Clothes> findByOwnerId(UUID ownerId);

    @Query("""
        select c from Clothes c
        left join fetch c.attributes a
        left join fetch a.definition d
        where c.id = :id
        """)
    Optional<Clothes> findByIdWithAttributes(UUID id);
}
