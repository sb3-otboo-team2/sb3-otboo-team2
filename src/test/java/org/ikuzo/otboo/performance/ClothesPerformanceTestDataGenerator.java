package org.ikuzo.otboo.performance;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.entity.AttributeOption;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttributeDef;
import org.ikuzo.otboo.domain.clothes.enums.ClothesType;
import org.ikuzo.otboo.domain.clothes.repository.ClothesAttributeDefRepository;
import org.ikuzo.otboo.domain.clothes.repository.ClothesRepository;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClothesPerformanceTestDataGenerator {

    private final UserRepository userRepository;
    private final ClothesRepository clothesRepository;
    private final ClothesAttributeDefRepository clothesAttributeDefRepository;
    private final Random random = new Random(42L);

    private static final ClothesType[] CLOTHES_TYPES = ClothesType.values();
    private static final String[] CLOTHES_NAMES = {
        "기본 티셔츠", "청바지", "원피스", "가디건", "코트", "운동화", "구두", "모자",
        "가방", "목도리", "스웨터", "셔츠", "치마", "반바지", "자켓", "부츠"
    };

    /** 외부에서 크기(userCount, clothesPerUser)를 받아 테스트 데이터를 생성하는 범용 메서드 */
    @Transactional
    public TestDataResult generateData(int userCount, int clothesPerUser) {
        log.info("=== 대량 테스트 데이터 생성 시작 (users={}, clothesPerUser={}) ===", userCount, clothesPerUser);
        long startTime = System.currentTimeMillis();

        // 0) 기본 속성 정의 생성 (이미 있으면 스킵)
        createBasicAttributeDefinitions();

        // 1) 사용자 생성
        List<User> users = createTestUsers(userCount);
        log.info("사용자 {} 명 생성 완료", users.size());

        // 2) 의상 생성
        List<Clothes> clothes = createTestClothes(users, clothesPerUser);
        log.info("의상 {} 개 생성 완료", clothes.size());

        long endTime = System.currentTimeMillis();

        TestDataResult result = new TestDataResult(
            users.size(),
            clothes.size(),
            endTime - startTime,
            users.stream().map(User::getId).toList()
        );

        log.info("=== 테스트 데이터 생성 완료 ===");
        log.info("사용자: {} 명, 의상: {} 개, 소요시간: {} ms",
            result.userCount(), result.clothesCount(), result.generationTimeMs());

        return result;
    }

    /** 기본 속성 정의 생성 (코드로 처리) */
    private void createBasicAttributeDefinitions() {
        log.info("기본 속성 정의 생성 시작");
        if (clothesAttributeDefRepository.count() > 0) {
            log.info("이미 속성 정의가 존재합니다. 스킵합니다.");
            return;
        }
        List<ClothesAttributeDef> attributeDefs = List.of(
            createAttributeDefWithOptions("스타일", List.of("포멀", "캐주얼", "미니멀", "시크", "러블리", "빈티지")),
            createAttributeDefWithOptions("소재", List.of("면", "폴리에스터", "울", "린넨", "데님", "가죽", "실크")),
            createAttributeDefWithOptions("계절", List.of("봄","여름","가을","겨울")),
            createAttributeDefWithOptions("두께", List.of("얇음", "보통", "두꺼움"))
        );
        clothesAttributeDefRepository.saveAll(attributeDefs);
        log.info("기본 속성 정의 {} 개 생성 완료", attributeDefs.size());
    }

    /** 속성 정의 + 옵션 생성 헬퍼 */
    private ClothesAttributeDef createAttributeDefWithOptions(String name, List<String> optionValues) {
        ClothesAttributeDef def = ClothesAttributeDef.builder().name(name).build();
        List<AttributeOption> options = optionValues.stream()
            .map(value -> AttributeOption.builder().value(value).definition(def).build())
            .toList();
        def.getOptions().addAll(options);
        return def;
    }

    private List<User> createTestUsers(int userCount) {
        log.info("사용자 {} 명 생성 시작", userCount);
        List<User> users = new ArrayList<>();
        List<User> batch = new ArrayList<>();

        for (int i = 1; i <= userCount; i++) {
            User user = new User(
                String.format("testuser%d@example.com", i),
                String.format("테스트사용자%d", i),
                "$2a$10$dummyhashedpassword"
            );
            batch.add(user);

            if (batch.size() >= 100) {
                userRepository.saveAll(batch);
                users.addAll(batch);
                batch.clear();
                if (i % 500 == 0) log.info("사용자 {} 명 생성 진행중...", i);
            }
        }
        if (!batch.isEmpty()) {
            userRepository.saveAll(batch);
            users.addAll(batch);
        }
        log.info("사용자 생성 완료: {} 명", users.size());
        return users;
    }

    private List<Clothes> createTestClothes(List<User> users, int clothesPerUser) {
        log.info("각 사용자당 {} 개의 의상 생성 시작", clothesPerUser);
        List<Clothes> allClothes = new ArrayList<>();
        List<Clothes> batch = new ArrayList<>();
        int totalCreated = 0;

        for (User user : users) {
            for (int clothesIndex = 1; clothesIndex <= clothesPerUser; clothesIndex++) {
                ClothesType randomType = CLOTHES_TYPES[random.nextInt(CLOTHES_TYPES.length)];
                String randomName = CLOTHES_NAMES[random.nextInt(CLOTHES_NAMES.length)];

                // 생성 시간을 다양화 (최근 1년 내 랜덤)
                Instant createdAt = Instant.now()
                    .minus(random.nextInt(365), ChronoUnit.DAYS)
                    .minus(random.nextInt(24), ChronoUnit.HOURS);

                Clothes clothes = Clothes.builder()
                    .name(String.format("%s_%s_%d", randomName, user.getName(), clothesIndex))
                    .type(randomType)
                    .owner(user)
                    .imageUrl(String.format("https://example.com/clothes/%s.jpg", UUID.randomUUID()))
                    .build();

                // 리플렉션으로 createdAt/updatedAt 설정 (테스트 전용)
                setCreatedAt(clothes, createdAt);

                batch.add(clothes);
                totalCreated++;

                if (batch.size() >= 1000) {
                    clothesRepository.saveAll(batch);
                    allClothes.addAll(batch);
                    batch.clear();
                    if (totalCreated % 5000 == 0) {
                        log.info("의상 {} 개 생성 진행중... (전체 예상: {} 개)", totalCreated, users.size() * clothesPerUser);
                    }
                }
            }
        }
        if (!batch.isEmpty()) {
            clothesRepository.saveAll(batch);
            allClothes.addAll(batch);
        }
        log.info("의상 생성 완료: {} 개", totalCreated);
        return allClothes;
    }

    /** 테스트를 위해 생성 시간을 엔티티에 주입 (BaseEntity에 createdAt/updatedAt 존재 가정) */
    private void setCreatedAt(Clothes clothes, Instant createdAt) {
        try {
            var baseClass = clothes.getClass().getSuperclass();
            var created = baseClass.getDeclaredField("createdAt");
            created.setAccessible(true);
            created.set(clothes, createdAt);

            var updated = baseClass.getDeclaredField("updatedAt");
            updated.setAccessible(true);
            updated.set(clothes, createdAt);
        } catch (Exception e) {
            log.warn("생성 시간 설정 실패: {}", e.getMessage());
        }
    }

    /** 테스트 데이터 생성 결과 DTO */
    public record TestDataResult(
        int userCount,
        int clothesCount,
        long generationTimeMs,
        List<UUID> userIds
    ) {}
}
