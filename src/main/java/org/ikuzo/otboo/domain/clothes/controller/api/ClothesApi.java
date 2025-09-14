package org.ikuzo.otboo.domain.clothes.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesUpdateRequest;
import org.ikuzo.otboo.global.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "의상 관리", description = "의상 관련 API")
public interface ClothesApi {

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
        ClothesCreateRequest request,
        MultipartFile image
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
        UUID clothesId,
        ClothesUpdateRequest request,
        MultipartFile image
    );

    @Operation(summary = "의상 삭제")
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "의상 삭제 성공",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ClothesDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "의상 삭제 실패",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<Void> delete(UUID clothesId);

}
