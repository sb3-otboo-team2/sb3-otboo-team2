package org.ikuzo.otboo.domain.user.repository;

import static org.ikuzo.otboo.domain.user.entity.QUser.user;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.user.entity.Role;
import org.ikuzo.otboo.domain.user.entity.User;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserCustomRepositoryImpl implements UserCustomRepository {

    private final JPAQueryFactory queryFactory; // QueryDSL 쿼리 빌더

    @Override
    public List<User> findUsersWithCursor(
        String cursor,
        UUID idAfter,
        Integer limit,
        String sortBy,
        String sortDirection,
        String emailLike,
        String roleEqual,
        Boolean locked
    ) {
        // WHERE 조건 빌더 생성
        BooleanBuilder whereClause = new BooleanBuilder();

        // 검색 필터 조건 추가
        // 이메일 검색: LIKE '%keyword%' (대소문자 무시)
        if (emailLike != null && !emailLike.isBlank()) {
            whereClause.and(user.email.containsIgnoreCase(emailLike));
        }

        // 역할 필터: role = 'ADMIN'
        if (roleEqual != null && !roleEqual.isBlank()) {
            try {
                Role role = Role.valueOf(roleEqual);
                whereClause.and(user.role.eq(role));
            } catch (IllegalArgumentException e) {
                // 잘못된 role 값이 들어오면 무시
            }
        }

        // 잠김 여부 필터: locked = true/false
        if (locked != null) {
            whereClause.and(user.locked.eq(locked));
        }

        // 커서 조건 추가 (다음 페이지 시작점 설정)
        if (cursor != null && idAfter != null) {
            BooleanExpression cursorCondition = buildCursorCondition(
                cursor, idAfter, sortBy, sortDirection
            );
            if (cursorCondition != null) {
                whereClause.and(cursorCondition);
            }
        }

        // 정렬 기준 결정
        OrderSpecifier<?> primaryOrder = getOrderSpecifier(sortBy, sortDirection);
        OrderSpecifier<UUID> secondaryOrder = user.id.asc(); // 동일 값 처리용 2차 정렬

        // 쿼리 실행
        return queryFactory
            .selectFrom(user)                          // SELECT * FROM users
            .where(whereClause)                        // WHERE 조건
            .orderBy(primaryOrder, secondaryOrder)     // ORDER BY sortBy, id
            .limit(limit + 1)                          // LIMIT (다음 페이지 존재 여부 확인용 +1)
            .fetch();                                  // 결과 리스트 반환
    }

    @Override
    public Long countUsersWithFilters(
        String emailLike,
        String roleEqual,
        Boolean locked
    ) {
        // 검색 조건만 동일하게 적용 (커서 제외)
        BooleanBuilder whereClause = new BooleanBuilder();

        if (emailLike != null && !emailLike.isBlank()) {
            whereClause.and(user.email.containsIgnoreCase(emailLike));
        }

        if (roleEqual != null && !roleEqual.isBlank()) {
            try {
                Role role = Role.valueOf(roleEqual);
                whereClause.and(user.role.eq(role));
            } catch (IllegalArgumentException e) {
                // 잘못된 role 무시
            }
        }

        if (locked != null) {
            whereClause.and(user.locked.eq(locked));
        }

        // 카운트 쿼리 실행
        return queryFactory
            .select(user.count())   // SELECT COUNT(*)
            .from(user)             // FROM users
            .where(whereClause)     // WHERE 조건
            .fetchOne();            // 단일 값 반환
    }

    /**
     * 커서 조건 생성 로직
     * "마지막으로 본 데이터 이후"를 나타내는 WHERE 조건
     *
     * 예시: email 내림차순 정렬 시
     * WHERE (email < 'last@email.com')
     *    OR (email = 'last@email.com' AND id > 'last-uuid')
     */
    private BooleanExpression buildCursorCondition(
        String cursor,
        UUID idAfter,
        String sortBy,
        String sortDirection
    ) {
        ComparableExpressionBase<?> sortField = getSortField(sortBy);
        boolean isDescending = "DESCENDING".equalsIgnoreCase(sortDirection);

        // email로 정렬하는 경우
        if (sortField.equals(user.email)) {
            if (isDescending) {
                // 내림차순: 마지막 email보다 작거나, 같으면 id가 큰 것
                return user.email.lt(cursor)
                    .or(user.email.eq(cursor).and(user.id.gt(idAfter)));
            } else {
                // 오름차순: 마지막 email보다 크거나, 같으면 id가 큰 것
                return user.email.gt(cursor)
                    .or(user.email.eq(cursor).and(user.id.gt(idAfter)));
            }
        }

        // createdAt으로 정렬하는 경우
        if (sortField.equals(user.createdAt)) {
            try {
                Instant instantCursor = Instant.parse(cursor); // ISO-8601 형식 파싱
                if (isDescending) {
                    return user.createdAt.lt(instantCursor)
                        .or(user.createdAt.eq(instantCursor).and(user.id.gt(idAfter)));
                } else {
                    return user.createdAt.gt(instantCursor)
                        .or(user.createdAt.eq(instantCursor).and(user.id.gt(idAfter)));
                }
            } catch (Exception e) {
                // 잘못된 날짜 형식이면 null 반환 (조건 무시)
                return null;
            }
        }

        // 기본: id만으로 커서 처리
        return user.id.gt(idAfter);
    }

    /**
     * 정렬 기준 필드 매핑
     *
     * @param sortBy "email", "createdAt" 등
     * @return QueryDSL 필드 표현식
     */
    private ComparableExpressionBase<?> getSortField(String sortBy) {
        return switch (sortBy) {
            case "email" -> user.email;
            case "createdAt" -> user.createdAt;
            default -> user.id; // 지원하지 않는 필드는 id 기본값
        };
    }

    /**
     * 정렬 방향 설정
     *
     * @param sortBy 정렬 기준 필드
     * @param sortDirection "ASCENDING" 또는 "DESCENDING"
     * @return OrderSpecifier (ASC/DESC)
     */
    private OrderSpecifier<?> getOrderSpecifier(String sortBy, String sortDirection) {
        ComparableExpressionBase<?> sortField = getSortField(sortBy);

        if ("DESCENDING".equalsIgnoreCase(sortDirection)) {
            return sortField.desc(); // 내림차순
        } else {
            return sortField.asc(); // 오름차순 (기본값)
        }
    }
}
