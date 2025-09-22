package org.ikuzo.otboo.global.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Slf4j
@Component
public class S3ImageStorage {

    private final S3Client s3Client;

    @Value("${AWS_S3_BUCKET}")
    private String bucketName;

    @Value("${AWS_S3_REGION}")
    private String region;

    public S3ImageStorage(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * 이미지 파일을 S3에 업로드
     *
     * @param imageFile  업로드할 이미지 파일
     * @param folderPath S3 내 저장 경로 (예: "profileImage/")
     * @return S3에 저장된 이미지 URL
     */
    public String uploadImage(MultipartFile imageFile, String folderPath) {
        log.info("이미지 업로드 시작 - 파일명: {}, 크기: {}바이트",
            imageFile.getOriginalFilename(), imageFile.getSize());

        // 파일 유효성 검증
        String finalMime = validateImageFile(imageFile);

        // 고유한 파일명 생성 (UUID + 타임스탬프)
        String originalFileName = imageFile.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "default.jpg";
        }

        String safeFileName = safeFileNameFromUrl(originalFileName);
        String uniqueFileName = generateUniqueFileName(safeFileName);

        // S3 저장 경로 생성 (폴더경로 + 파일명)
        String s3Key = folderPath + uniqueFileName;

        // getContentType()이 image/*가 아니거나 null/octal이면 미리 계산한 값으로 대체
        String requestMime = imageFile.getContentType();
        if (requestMime == null || !requestMime.toLowerCase().startsWith("image/")) {
            requestMime = finalMime;
        }

        // S3 업로드 요청 객체 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .contentType(requestMime)
            .contentLength(imageFile.getSize())
            .build();

        try {
            // S3에 파일 업로드 실행
            PutObjectResponse response = s3Client.putObject(
                putObjectRequest,
                RequestBody.fromBytes(imageFile.getBytes())
            );

            // 업로드된 파일의 공개 URL 생성
            String publicUrl = generatePublicUrl(s3Key);

            log.info("이미지 업로드 완료 - S3 Key: {}, ETag: {}", s3Key, response.eTag());
            return publicUrl;

        } catch (Exception e) {
            log.error("이미지 업로드 실패 - 파일명: {}, 오류: {}", imageFile.getOriginalFilename(),
                e.getMessage());
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
        }
    }

    private String safeFileNameFromUrl(String url) {
        String path = (url == null) ? "" : url;
        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);        // ? 이하 제거
        // 마지막 / 뒤만 취해서 파일명만 사용
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        // 너무 길거나 위험한 문자 정리
        fileName = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        return fileName;
    }

    /**
     * 이미지 파일 유효성 검증
     */
    private String validateImageFile(MultipartFile file) {
        // 파일이 비어있는지 확인
        if (file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지 파일이 없습니다.");
        }

        // 파일 타입이 이미지인지 확인
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        String extMime = MimeTypeResolver.resolveFromExtension(safeFileNameFromUrl(fileName),
            "application/octet-stream");
        boolean declaredImage = contentType != null && contentType.startsWith("image/");
        boolean inferredImage = extMime != null && extMime.startsWith("image/");
        if (!(declaredImage || inferredImage)) {
            throw new IllegalArgumentException(
                "이미지 파일만 업로드 가능합니다. 현재 타입: " + contentType + ", 추정: " + extMime);
        }
        String finalMime = (declaredImage ? contentType : extMime).toLowerCase();

        // 파일 크기 제한 (10MB)
        long maxSizeInBytes = 10 * 1024 * 1024;
        if (file.getSize() > maxSizeInBytes) {
            throw new IllegalArgumentException("이미지 파일 크기는 10MB를 초과할 수 없습니다. 현재 크기: " + file.getSize() + "바이트");
        }

        // 지원하는 이미지 형식인지 확인
        String[] supportedTypes = {"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
            "image/bmp", "image/svg+xml", "image/x-icon", "image/tiff", "application/octet-stream"};
        boolean isSupported = false;
        for (String type : supportedTypes) {
            if (type.equalsIgnoreCase(finalMime)) {
                isSupported = true;
                break;
            }
        }
        if (!isSupported) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. 지원 형식: JPEG, JPG, PNG, WebP");
        }

        return extMime;
    }

    /**
     * 고유한 파일명 생성 (중복 방지)
     */
    private String generateUniqueFileName(String originalFileName) {
        // 파일 확장자 추출
        String extension = getFileExtension(originalFileName);

        // 현재 시간을 문자열로 변환 (예: 20250711_103022)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        // UUID의 앞 8자리만 사용 (너무 길지 않게)
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        // 최종 파일명: uuid_timestamp.확장자
        return String.format("%s_%s.%s", uuid, timestamp, extension);
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "jpg";  // 기본 확장자
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * S3 공개 URL 생성
     */
    private String generatePublicUrl(String s3Key) {
        // S3 공개 URL 형태
        // https://버킷명.s3.리전.amazonaws.com/파일경로
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
    }
    /**
     * S3에 업로드된 이미지 삭제
     * @param imageUrl 삭제할 이미지의 공개 URL
     */
    public void deleteImage(String imageUrl) {
        if(imageUrl == null || imageUrl.isBlank()) {
            log.warn("삭제할 이미지 URL이 비어 있습니다.");
            return;
        }
        // URL에서 S3 key 추출
        String key = extractKeyFromUrl(imageUrl);

        log.info("이미지 삭제 시작 - S3 Key: {}", key);

        try {
            s3Client.deleteObject(builder -> builder
                .bucket(bucketName)
                .key(key)
                .build()
            );
            log.info("이미지 삭제 완료 - S3 Key: {}", key);
        } catch (Exception e) {
            log.error("이미지 삭제 실패 - S3 Key: {}, 오류: {}", key, e.getMessage());
            throw new RuntimeException("이미지 삭제 중 오류가 발생했습니다.", e);
        }
    }
    /**
     * 공개 URL에서 S3 key 경로 추출
     * 예: https://bucket-name.s3.region.amazonaws.com/folder/filename.jpg
     * → folder/filename.jpg
     */
    private String extractKeyFromUrl(String url) {
        String baseUrl = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        if (!url.startsWith(baseUrl)) {
            throw new IllegalArgumentException("유효한 S3 공개 URL이 아닙니다.");
        }
        String key = url.substring(baseUrl.length());
        if(key.isEmpty()){
            throw new IllegalArgumentException("S3 key가 비어 있습니다.");
        }
        return key;
    }
}
