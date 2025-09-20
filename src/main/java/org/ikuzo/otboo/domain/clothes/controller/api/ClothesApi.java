package org.ikuzo.otboo.domain.clothes.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesUpdateRequest;
import org.ikuzo.otboo.global.dto.PageResponse;
import org.ikuzo.otboo.global.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@Tag(name = "의상 관리", description = "의상 관련 API")
public interface ClothesApi {

    @Operation(summary = "의상 목록 조회")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "의상 목록 조회 성공",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ClothesDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "의상 목록 조회 실패",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<PageResponse<ClothesDto>> getWithCursor(
        @RequestParam UUID ownerId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
        @RequestParam(required = false) String typeEqual
    );

    @Operation(summary = "의상 등록")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "의상 등록 성공",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ClothesDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "의상 등록 실패",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<ClothesDto> create(
        @RequestPart("request") @Valid ClothesCreateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image
    );

    @Operation(summary = "의상 수정")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "의상 수정 성공",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ClothesDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "의상 수정 실패",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<ClothesDto> update(
        @PathVariable("clothesId") UUID clothesId,
        @RequestPart("request") @Valid ClothesUpdateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image
    );

    @Operation(summary = "의상 삭제")
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "의상 삭제 성공",
            content = @Content()
        ),
        @ApiResponse(
            responseCode = "404",
            description = "의상 삭제 실패",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<Void> delete(
        @PathVariable("clothesId") UUID clothesId
    );

    @Operation(summary = "구매 링크로 옷 정보 불러오기")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "구매 링크로 옷 정보 불러오기 성공",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ClothesDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "구매 링크로 옷 정보 불러오기 실패",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    Mono<ResponseEntity<ClothesDto>> extractByUrl(
        @RequestParam("url") String url
    );

}
