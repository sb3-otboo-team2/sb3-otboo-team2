package org.ikuzo.otboo.domain.clothes.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDefDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefUpdateRequest;
import org.ikuzo.otboo.domain.clothes.enums.AttributeDefSortBy;
import org.ikuzo.otboo.domain.clothes.enums.AttributeDefSortDirection;
import org.ikuzo.otboo.global.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "의상 속성 정의", description = "의상 속성 정의 관련 API")
public interface ClothesAttributeDefApi {

    @Operation(summary = "의상 속성 정의 목록 조회")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "의상 속성 정의 목록 조회 성공",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ClothesAttributeDefDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "의상 속성 정의 목록 조회 실패",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<List<ClothesAttributeDefDto>> getWithCursor(
        @RequestParam AttributeDefSortBy sortBy,
        @RequestParam AttributeDefSortDirection sortDirection,
        @RequestParam(required = false, defaultValue = "") String keywordLike
    );

    @Operation(summary = "의상 속성 정의 등록")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "의상 속성 정의 등록 성공",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ClothesAttributeDefDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "의상 속성 정의 등록 실패",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<ClothesAttributeDefDto> create(
        @RequestBody @Valid ClothesAttributeDefCreateRequest request
    );

    @Operation(summary = "의상 속성 정의 수정")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "의상 속성 정의 수정 성공",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ClothesAttributeDefDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "의상 속성 정의 수정 실패",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<ClothesAttributeDefDto> update(
        @PathVariable UUID definitionId,
        @RequestBody @Valid ClothesAttributeDefUpdateRequest request
    );

    @Operation(summary = "의상 속성 정의 삭제")
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "의상 속성 정의 삭제 성공",
            content = @Content()
        ),
        @ApiResponse(
            responseCode = "400",
            description = "의상 속성 정의 삭제 실패",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<Void> delete(
        @PathVariable UUID definitionId
    );

}
