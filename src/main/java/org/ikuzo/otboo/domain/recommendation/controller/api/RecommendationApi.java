package org.ikuzo.otboo.domain.recommendation.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.ikuzo.otboo.domain.recommendation.dto.RecommendationDto;
import org.ikuzo.otboo.global.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "추천 관리", description = "추천 관련 API")
public interface RecommendationApi {

    @Operation(summary = "추천 조회")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "추천 조회 성공",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = RecommendationDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "추천 조회 실패",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<RecommendationDto> create(
        @RequestParam UUID weatherId
    );

}
