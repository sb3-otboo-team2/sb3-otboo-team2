package org.ikuzo.otboo.domain.clothes.repository;

import java.util.List;
import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;

public interface ClothesRepositoryCustom {

    List<Clothes> findClothesWithCursor(
        UUID ownerId,
        String cursor,
        UUID idAfter,
        int limit,
        String typeEqual
    );

    Long countClothes(UUID ownerId, String typeEqual);

}
