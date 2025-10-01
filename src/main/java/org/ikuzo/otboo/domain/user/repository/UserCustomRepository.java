package org.ikuzo.otboo.domain.user.repository;

import java.util.List;
import java.util.UUID;
import org.ikuzo.otboo.domain.user.entity.User;

public interface UserCustomRepository {
    /**
     * 커서 기반 페이지네이션으로 사용자 목록 조회
     *
     * @param cursor 마지막으로 본 정렬 기준 값 (email 또는 createdAt)
     * @param idAfter 마지막으로 본 사용자의 UUID (동일한 cursor 값 처리용)
     * @param limit 한 페이지에 가져올 개수
     * @param sortBy 정렬 기준 (email, createdAt)
     * @param sortDirection 정렬 방향 (ASCENDING, DESCENDING)
     * @param emailLike 이메일 검색어 (부분 검색)
     * @param roleEqual 역할 필터 (USER, ADMIN)
     * @param locked 계정 잠김 여부 필터
     * @return 조회된 사용자 목록 (limit+1개)
     */
    List<User> findUsersWithCursor(
        String cursor,
        UUID idAfter,
        Integer limit,
        String sortBy,
        String sortDirection,
        String emailLike,
        String roleEqual,
        Boolean locked
    );

    /**
     * 필터 조건에 맞는 전체 사용자 수 카운트
     *
     * @param emailLike 이메일 검색어
     * @param roleEqual 역할 필터
     * @param locked 잠김 여부 필터
     * @return 조건에 맞는 총 사용자 수
     */
    Long countUsersWithFilters(
        String emailLike,
        String roleEqual,
        Boolean locked
    );
}
