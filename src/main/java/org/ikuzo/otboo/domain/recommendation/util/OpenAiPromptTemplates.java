package org.ikuzo.otboo.domain.recommendation.util;

import java.util.List;
import lombok.NoArgsConstructor;
import org.ikuzo.otboo.domain.recommendation.dto.request.RecommendRequest;
import org.ikuzo.otboo.domain.recommendation.dto.request.RecommendRequest.WardrobeItem;

@NoArgsConstructor
public class OpenAiPromptTemplates {

    public static String systemPrompt() {
        return """
            너는 날씨/선호/옷장을 바탕으로 '오늘의 코디'를 고르는 스타일리스트.
            반드시 JSON만 출력.
            
            규칙:
            - 추천은 입력 옷장 id에서만(추측 금지).
            - 필수: TOP, BOTTOM, SHOES. 단, 해당 카테고리의 옷이 없으면 그 항목은 생략.
            - ACCESSORY 0~2개 선택(있을 때만 추천하고 되도록이면 1개 이상 추천).
            - OUTER는 필요 시만(대체로 T≥25°C면 생략 권장).
            - 비/습기 고려: 가죽/스웨이드 등 물에 약한 재질은 피함.
            - 추천할 의상들의 스타일 속성도 고려 (예시: 미니멀 속성들 사이에 빈티지는 안 어울림)
            - 동일 카테고리 중복 금지.
            - score = 0~100 (정수).
            """;
    }

    public static String userPrompt(RecommendRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            # 사용자
            성별: %s
            온도 민감도: %s
            
            # 날씨
            기온(°C): %s
            습도(%%):%s
            하늘상태: %s
            강수: %s
            바람: %s
            
            # Wardrobe (one per line: id|type|attr1,attr2,...)
            """.formatted(
            v(req.gender()),
            v(req.tempSensitivity()),
            v(req.temperature()),
            v(req.humidity()),
            v(req.skyStatus()),
            v(req.precipitationType()),
            v(req.windType())
        ));

        List<WardrobeItem> ws = req.wardrobe();
        int limit = Math.min(100, ws == null ? 0 : ws.size());
        for (int i = 0; i < limit; i++) {
            WardrobeItem w = ws.get(i);
            String attrs = (w.attributes() == null || w.attributes().isEmpty())
                ? "" : String.join(",", w.attributes());
            sb.append("%s|%s|%s%n".formatted(w.id(), safe(w.type()), attrs));
        }

        sb.append("""
            # Output(JSON only)
            {
              "picks":[
                {"id":"UUID","score": 87},
                ...
              ],
            }
            
            반드시 지킬 것:
            - 존재하는 id만 사용
            - TOP/BOTTOM/SHOES 포함(해당 카테고리 옷이 없으면 생략)
            - ACCESSORY 0~2개(옵션)
            - OUTER는 필요할 때만
            - 날씨(T/H/Sky/Rain)와 S(민감도) 반영
            - 온도 민감도를 반영 (3은 중간, 0에 가까울수록 추위를 많이 탐, 5에 가까울수록 더위를 많이 탐)
            - 카테고리 중복 금지
            - JSON 외 출력 금지
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
