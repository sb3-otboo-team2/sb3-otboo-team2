package org.ikuzo.otboo.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import org.ikuzo.otboo.domain.clothes.service.ClothesService;
import org.ikuzo.otboo.global.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * 실행 가이드
 * 1) 전체 비교:   ./gradlew test --tests "*ClothesPerformanceTest.testPerformanceComparison"
 * 2) 인덱스 없음: ./gradlew test --tests "*ClothesPerformanceTest.testClothesListPerformanceWithoutIndex"
 * 3) 인덱스 적용: ./gradlew test --tests "*ClothesPerformanceTest.testClothesListPerformanceWithIndex"

 * 크기 변경:
 *   ./gradlew test -Dperf.users=2000 -Dperf.clothesPerUser=15 --tests "*ClothesPerformanceTest.testPerformanceComparison" --info
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ClothesPerformanceTest {

    private static final Path DESKTOP_DIR = Path.of("C:", "Users", "sjo06", "OneDrive", "바탕 화면");

    @Autowired private ClothesPerformanceTestDataGenerator testDataGenerator;
    @Autowired private ClothesService clothesService;
    @Autowired private JdbcTemplate jdbcTemplate;

    // 외부 주입 값 (없으면 기본값 사용)
    @Value("${perf.users:1000}")
    private int perfUsers;

    @Value("${perf.clothesPerUser:30}")
    private int perfClothesPerUser;

    // (선택) 한 번의 측정에서 수행할 요청 수 상한. 기본은 "사용자 수"만큼 1회씩.
    @Value("${perf.maxRequestsPerRun:-1}") // -1이면 사용자 수와 동일
    private int maxRequestsPerRun;

    private ClothesPerformanceTestDataGenerator.TestDataResult testData;

    @BeforeEach
    void setUp() {
        log.info("=== 성능 테스트 데이터 준비 (users={}, clothesPerUser={}) ===", perfUsers, perfClothesPerUser);
        testData = testDataGenerator.generateData(perfUsers, perfClothesPerUser);
    }

    @Test
    @DisplayName("인덱스 적용 전후 성능 비교 (나노초 집계)")
    void testPerformanceComparison() {
        log.info("=== 인덱스 적용 전후 성능 비교 테스트 ===");

        // 1) 인덱스 전
        dropIndexes();
        PerformanceResult before = measureClothesListPerformance("인덱스 적용 전");
        writeCsv(before);

        // 2) 인덱스 후
        createOptimizedIndexes();
        PerformanceResult after = measureClothesListPerformance("인덱스 적용 후");
        writeCsv(after);

        // 3) 비교 출력
        printComparisonResult(before, after);
        writePrettyComparisonToDesktop(before, after);

        // 4) 최소 개선(10%) 검증
        double improve = (before.avgMs() - after.avgMs()) / before.avgMs();
        log.info("성능 개선율: {}%", String.format("%.1f", improve * 100.0));
        assertThat(improve).isGreaterThan(0.10);
    }

    /** 한 번의 측정에서 실제 실행할 요청 수 계산: 기본은 "사용자 수" */
    private int effectiveMeasureCount() {
        int users = testData.userCount();
        if (maxRequestsPerRun <= 0) return users;
        return Math.min(users, maxRequestsPerRun);
    }

    /** 의상 목록 조회 성능 측정 (나노초 집계, 워밍업 분리) */
    private PerformanceResult measureClothesListPerformance(String testName) {
        List<UUID> userIds = new ArrayList<>(testData.userIds());
        Collections.shuffle(userIds, new Random(42)); // 재현성

        final int measure = effectiveMeasureCount();
        final int warmup = Math.min(Math.max(measure / 5, 50), Math.max(1, measure)); // 최대 20% 또는 50회

        // ---- 워밍업 (측정 제외) ----
        for (int i = 0; i < warmup; i++) {
            UUID uid = userIds.get(i % userIds.size());
            clothesService.getWithCursor(uid, null, null, 20, null);
        }

        // ---- 본 측정 ----
        List<Long> nanosList = new ArrayList<>(measure);
        for (int i = 0; i < measure; i++) {
            UUID userId = userIds.get(i % userIds.size());
            long t0 = System.nanoTime();
            PageResponse<ClothesDto> page = clothesService.getWithCursor(userId, null, null, 20, null);
            long t1 = System.nanoTime();

            nanosList.add(t1 - t0);

            assertThat(page).isNotNull();
            assertThat(page.data()).isNotNull();

            if ((i + 1) % Math.max(100, measure / 5) == 0 || i + 1 == measure) {
                log.info("{} - 진행률: {}/{} ({}%)",
                    testName, i + 1, measure, String.format("%.1f", 100.0 * (i + 1) / measure));
            }
        }

        return calculatePerformanceMetricsFromNanos(nanosList, testName);
    }

    /** 성능 지표 계산 (입력: 나노초, 출력: ms double) */
    private PerformanceResult calculatePerformanceMetricsFromNanos(List<Long> nanosList, String testName) {
        nanosList.sort(Long::compareTo);

        long totalNanos = nanosList.stream().mapToLong(Long::longValue).sum();
        double avgMs = totalNanos / 1_000_000.0 / nanosList.size();
        double minMs = nanosList.get(0) / 1_000_000.0;
        double maxMs = nanosList.get(nanosList.size() - 1) / 1_000_000.0;

        double p50Ms = percentileNanos(nanosList, 50) / 1_000_000.0;
        double p90Ms = percentileNanos(nanosList, 90) / 1_000_000.0;
        double p95Ms = percentileNanos(nanosList, 95) / 1_000_000.0;
        double p99Ms = percentileNanos(nanosList, 99) / 1_000_000.0;

        double totalMs = totalNanos / 1_000_000.0;

        return new PerformanceResult(
            testName,
            testData.userCount(),           // ➜ 데이터 크기(사용자 수)
            testData.clothesCount(),        // ➜ 데이터 크기(의상 수)
            nanosList.size(),               // 총 실행 횟수
            avgMs, minMs, maxMs,
            p50Ms, p90Ms, p95Ms, p99Ms,
            totalMs
        );
    }

    private static long percentileNanos(List<Long> sortedNanos, int p) {
        int n = sortedNanos.size();
        int rank = (int) Math.ceil((p / 100.0) * n);
        rank = Math.min(Math.max(rank, 1), n);
        return sortedNanos.get(rank - 1);
    }

    /** 최소 인덱스 세트 (쿼리 패턴: owner_id [AND type] + created_at DESC, id DESC) */
    private void createOptimizedIndexes() {
        String[] creates = {
            "CREATE INDEX IF NOT EXISTS idx_clothes_owner_created_type ON clothes (owner_id, created_at DESC, type)",
            "CREATE INDEX IF NOT EXISTS idx_clothes_owner_id ON clothes (owner_id)",
            "CREATE INDEX IF NOT EXISTS idx_clothes_owner_type ON clothes (owner_id, type)",
            "CREATE INDEX IF NOT EXISTS idx_clothes_type_owner_created ON clothes (type, owner_id, created_at DESC)"
        };
        for (String q : creates) {
            try { jdbcTemplate.execute(q); log.info("인덱스 생성 완료: {}", q); }
            catch (Exception e) { log.error("인덱스 생성 실패: {}", q, e); }
        }
        log.info("최소 인덱스 세트 적용 완료");
    }


    private void dropIndexes() {
        String[] drops = {
            "DROP INDEX IF EXISTS idx_clothes_owner_created_type",
            "DROP INDEX IF EXISTS idx_clothes_owner_id",
            "DROP INDEX IF EXISTS idx_clothes_owner_type",
            "DROP INDEX IF EXISTS idx_clothes_type_owner_created",
        };
        for (String q : drops) {
            try { jdbcTemplate.execute(q); log.info("인덱스 제거 완료: {}", q); }
            catch (Exception e) { log.warn("인덱스 제거 실패(무시 가능): {}", q, e); }
        }
    }

    /** 비교 출력 (소수점 표시 + 데이터 크기 표시) */
    private void printComparisonResult(PerformanceResult before, PerformanceResult after) {
        double thrBefore = before.totalExecutions() / (before.totalMs() / 1000.0);
        double thrAfter  = after.totalExecutions()  / (after.totalMs()  / 1000.0);
        double thrImprove = (thrAfter - thrBefore) / thrBefore * 100.0;

        log.warn("==================== 성능 비교 결과 ====================");
        log.warn("데이터 크기: 사용자 {}명, 의상 {}개", before.userCount(), before.clothesCount());
        log.warn("구분\t\t\t인덱스 전\t\t인덱스 후\t\t개선율");
        log.warn("--------------------------------------------------------");
        printComparisonLine("평균 응답시간 (ms)", before.avgMs(), after.avgMs());
        printComparisonLine("90% 응답시간 (ms)", before.p90Ms(), after.p90Ms());
        printComparisonLine("95% 응답시간 (ms)", before.p95Ms(), after.p95Ms());
        printComparisonLine("99% 응답시간 (ms)", before.p99Ms(), after.p99Ms());
        printComparisonLine("최대 응답시간 (ms)", before.maxMs(), after.maxMs());
        log.warn("처리량 (req/sec)\t\t{}\t\t{}\t\t{}",
            String.format("%.2f", thrBefore),
            String.format("%.2f", thrAfter),
            String.format("%+.1f%%", thrImprove));
        log.warn("========================================================");
    }

    private void printComparisonLine(String metric, double before, double after) {
        double improve = (before - after) / before * 100.0;
        log.warn("{}\t\t{}\t\t{}\t\t{}",
            metric,
            String.format("%.3f", before),
            String.format("%.3f", after),
            String.format("%+.1f%%", improve));
    }

    /** CSV 저장 (데이터 크기 포함) */
    private void writeCsv(PerformanceResult r) {
        try {
            Path dir = Path.of("build", "reports", "tests", "perf");
            Files.createDirectories(dir);
            Path path = dir.resolve("clothes_compare.csv");
            String line = String.join(",",
                r.testName(),
                String.valueOf(r.userCount()),
                String.valueOf(r.clothesCount()),
                String.valueOf(r.totalExecutions()),
                String.format("%.6f", r.avgMs()),
                String.format("%.6f", r.p50Ms()),
                String.format("%.6f", r.p90Ms()),
                String.format("%.6f", r.p95Ms()),
                String.format("%.6f", r.p99Ms()),
                String.format("%.6f", r.maxMs()),
                String.format("%.6f", r.totalMs())
            ) + System.lineSeparator();
            Files.writeString(path, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.warn("CSV 기록 실패(무시 가능): {}", e.toString());
        }
    }

    private Path resolveDesktop(String fileName) throws Exception {
        Files.createDirectories(DESKTOP_DIR);
        return DESKTOP_DIR.resolve(fileName);
    }


    // 로그 포맷 그대로 바탕화면에 저장 (append)
    private void writePrettyComparisonToDesktop(PerformanceResult before, PerformanceResult after) {
        try {
            double thrBefore = before.totalExecutions() / (before.totalMs() / 1000.0);
            double thrAfter  = after.totalExecutions()  / (after.totalMs()  / 1000.0);
            double thrImprove = (thrAfter - thrBefore) / thrBefore * 100.0;

            StringBuilder sb = new StringBuilder();
            sb.append("==================== 성능 비교 결과 ====================\n");
            sb.append(String.format("데이터 크기: 사용자 %d명, 의상 %d개%n", before.userCount(), before.clothesCount()));
            sb.append("구분\t\t\t인덱스 전\t\t인덱스 후\t\t개선율\n");
            sb.append("--------------------------------------------------------\n");
            sb.append(formatLine("평균 응답시간 (ms)", before.avgMs(), after.avgMs()));
            sb.append(formatLine("90% 응답시간 (ms)", before.p90Ms(), after.p90Ms()));
            sb.append(formatLine("95% 응답시간 (ms)", before.p95Ms(), after.p95Ms()));
            sb.append(formatLine("99% 응답시간 (ms)", before.p99Ms(), after.p99Ms()));
            sb.append(formatLine("최대 응답시간 (ms)", before.maxMs(), after.maxMs()));
            sb.append(String.format("처리량 (req/sec)\t\t%s\t\t%s\t\t%s%n",
                String.format("%.2f", thrBefore),
                String.format("%.2f", thrAfter),
                String.format("%+.1f%%", thrImprove)));
            sb.append("========================================================\n\n");

            Path out = resolveDesktop("clothes_perf_pretty.txt"); // 원하는 파일명
            Files.writeString(out, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("바탕화면 파일 저장 완료: {}", out.toAbsolutePath());
        } catch (Exception e) {
            log.warn("바탕화면 파일 저장 실패: {}", e.toString());
        }
    }

    // 표 한 줄 포맷(로그와 동일 탭/정밀도)
    private String formatLine(String label, double before, double after) {
        double improve = (before - after) / Math.max(before, 1e-9) * 100.0;
        return String.format("%s\t\t%s\t\t%s\t\t%s%n",
            label,
            String.format("%.3f", before),
            String.format("%.3f", after),
            String.format("%+.1f%%", improve));
    }

    /** 결과 DTO (데이터 크기 + ms double) */
    public record PerformanceResult(
        String testName,
        int    userCount,       // ➜ 추가: 데이터셋 사용자 수
        int    clothesCount,    // ➜ 추가: 데이터셋 의상 수
        int    totalExecutions,
        double avgMs,
        double minMs,
        double maxMs,
        double p50Ms,
        double p90Ms,
        double p95Ms,
        double p99Ms,
        double totalMs
    ) {}
}
