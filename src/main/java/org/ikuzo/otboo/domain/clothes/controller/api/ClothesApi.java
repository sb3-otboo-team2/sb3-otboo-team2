package org.ikuzo.otboo.domain.clothes.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDefDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefCreateRequest;
import org.ikuzo.otboo.global.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "의상 속성 정의", description = "의상 속성 정의 관련 API")
public interface ClothesApi {

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
        ClothesAttributeDefCreateRequest userRegisterRequest
    );

}
