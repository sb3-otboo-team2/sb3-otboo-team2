package org.ikuzo.otboo.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Slf4j
@Component
public class S3LogStorage {

    private final S3Client s3Client;

    @Value("${AWS_S3_BUCKET}")
    private String bucket;

    private final Path logDir = Paths.get(".logs");

    public S3LogStorage(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void uploadLogs() {
        try (Stream<Path> files = Files.walk(logDir)) {
            log.info("Uploading logs to S3 bucket {}", bucket);
            files
                .filter(Files::isRegularFile)
                .filter(this::isDatePatternLogFile)
                .forEach(this::upload);
        } catch (IOException e) {
            log.error("로그 파일 탐색 실패", e);
        }
    }

    private void upload(Path path) {
        String key = "logs/" + path.getFileName();

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("text/plain")
                .build();

            s3Client.putObject(request, path);

            log.info("S3 업로드 완료: {}", key);

            // 업로드 후 삭제
            Files.deleteIfExists(path);

        } catch (Exception e) {
            log.error("S3 업로드 실패 - {}", path, e);
        }
    }

    private boolean isDatePatternLogFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.matches("application\\.\\d{4}-\\d{2}-\\d{2}\\.log");
    }
}
