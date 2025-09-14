package org.ikuzo.otboo.global.util;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageSwapHelper {


    private final S3ImageStorage s3ImageStorage;

    /**
     * 트랜잭션 안전한 이미지 교체
     * - 새 이미지 업로드
     * - 트랜잭션 커밋 성공 시: 기존 이미지 삭제
     * - 트랜잭션 롤백 시: 새 이미지 삭제
     */
    public String swapImageSafely(MultipartFile newImage, String oldImageUrl, UUID ownerId) {
        if (newImage == null || newImage.isEmpty()) {
            return null;
        }

        String folder = "clothes/" + ownerId + "/";
        String newImageUrl = s3ImageStorage.uploadImage(newImage, folder);

        registerCleanupCallbacks(newImageUrl, oldImageUrl);

        return newImageUrl;
    }

    private void registerCleanupCallbacks(String newImageUrl, String oldImageUrl) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("활성 트랜잭션이 없어 이미지 스왑 콜백을 등록하지 않습니다.");
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (oldImageUrl != null) {
                        cleanupImageQuietly(oldImageUrl, "기존 이미지 정리");
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        cleanupImageQuietly(newImageUrl, "롤백 시 새 이미지 정리");
                    }
                }
            }
        );
    }

    private void cleanupImageQuietly(String imageUrl, String context) {
        if (imageUrl == null) {
            return;
        }
        try {
            s3ImageStorage.deleteImage(imageUrl);
            log.debug("{} 완료: {}", context, imageUrl);
        } catch (Exception ex) {
            log.error("{} 실패: {}", context, imageUrl, ex);
        }
    }
}
