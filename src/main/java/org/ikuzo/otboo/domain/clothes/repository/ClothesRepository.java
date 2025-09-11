package org.ikuzo.otboo.domain.clothes.repository;

import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesRepository extends JpaRepository<Clothes, UUID> {

}
