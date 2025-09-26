package org.ikuzo.otboo.domain.recommendation.util;

import java.util.List;
import lombok.NoArgsConstructor;
import org.ikuzo.otboo.domain.recommendation.dto.request.RecommendRequest;
import org.ikuzo.otboo.domain.recommendation.dto.request.RecommendRequest.WardrobeItem;

@NoArgsConstructor
public class OpenAiPromptTemplates {

    public static String systemPrompt() {
        return """
        너는 날씨/사용자 선호/옷장 정보를 바탕으로 오늘의 의상을 고르는 전문 스타일리스트다.
        반드시 JSON으로만 출력한다. 한국어로 간단한 이유를 포함한다.

        하드 규칙(반드시 지켜라):
        - 추천은 오직 제공된 옷장 목록의 id에서만 고른다. 추측/창작 금지.
        - 필수 카테고리: TOP(상의), BOTTOM(하의), SHOES(신발). 이 셋은 존재하도록 추천한다.
        - 필수 카테고리 셋 중에 존재하지 의상이 존재한다면 해당 카테고리는 제외하고 추천한다 .
        - ACCESSORY(악세서리)는 있으면 0~2개 범위에서 스타일에 맞게 추천할 수 있다(없으면 생략).
        - OUTER(아우터)는 날씨를 고려하여 필요할 때만 포함한다(온도가 25°C 이상이면 생략 권장).
        - 날씨를 고려하여 의상의 재질 및 두께를 추천에 반영 (ex: 비가 오면 되도록 가죽 & 스웨이드 추천하지 않는다).
        - 같은 카테고리에서 중복 추천하지 않는다(예: 상의 2개 X).
        - score는 0~100 사이 정수.
        """;
    }

    public static String userPrompt(RecommendRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            # 사용자
            - 성별: %s
            - 온도민감도(0=추위,5=더위): %s
            
            # 날씨
            - 기온(°C): %s
            - 습도(%%): %s
            - 하늘상태: %s
            - 강수: %s
            
            # 옷장(간단 목록)
            """.formatted(
            v(req.gender()),
            v(req.tempSensitivity()),
            v(req.temperature()),
            v(req.humidity()),
            v(req.skyStatus()),
            v(req.precipitationType())
        ));

        List<WardrobeItem> ws = req.wardrobe();
        int limit = Math.min(50, ws == null ? 0 : ws.size());
        for (int i = 0; i < limit; i++) {
            var w = ws.get(i);
            sb.append("- [%s] %s / %s / %s%n".formatted(
                w.id(),
                safe(w.name()),
                safe(w.type()),
                w.attributes() == null ? "" : String.join(", ", w.attributes())
            ));
        }

        sb.append("""
                # 출력 형식(JSON)
                {
                  "picks": [
                    {"id": "UUID", "score": 0.0~1.0, "reason": "짧고 명확한 이유"},
                    ...
                  ],
                  "reasoning": "전체 추천에 대한 한 문단 요약"
                }
            
                규칙:
                - 반드시 JSON만 출력
                - 존재하는 id만 사용
                - score는 0~100 사이 정수
                - 상의/하의/아우터 등 조합을 1~3개 추천(너무 길지 않게)
                - 날씨(기온/습도/강수)와 사용자의 온도민감도를 반드시 반영
                - TOP, BOTTOM, SHOES는 모두 포함되어야 한다.
                - ACCESSORY는 있으면 0~2개 한도에서 추가할 수 있다(없으면 생략).
                - OUTER는 필요할 때만 포함(더우면 생략).
                - 같은 카테고리에서 중복 선택 금지.
                - 존재하는 id만 사용하고, 응답엔 JSON만 포함한다.
            """);
        return sb.toString();
    }

    private static String v(Object o) {
        return o == null ? "N/A" : o.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

}
