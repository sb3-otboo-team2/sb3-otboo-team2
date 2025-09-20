package org.ikuzo.otboo.domain.clothes.extractions;

import static org.apache.commons.lang3.StringUtils.firstNonBlank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.clothes.config.OpenAiProps;
import org.ikuzo.otboo.domain.clothes.infrastructure.ExtractedClothing;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


@Component
@RequiredArgsConstructor
public class OpenAiHtmlExtractor {

    private final OpenAiProps props;
    private final OpenAiChatClient chatClient;
    private final ObjectMapper om = new ObjectMapper();


    public Mono<ExtractedClothing> extract(URI uri, String fullHtml,
        Map<String, List<String>> allowedDefs) {
        Document doc = Jsoup.parse(fullHtml);
        doc.select("script, style, noscript").remove();
        String title = Optional.ofNullable(doc.selectFirst("meta[property=og:title]"))
            .map(e -> e.attr("content")).orElse(doc.title());
        String image = Optional.ofNullable(doc.selectFirst("meta[property=og:image]"))
            .map(e -> e.attr("content")).orElse(null);
        String text = doc.text();
        if (text.length() > 6000)
            text = text.substring(0, 6000);

        String system = """
            당신은 전자상거래 상품 페이지에서 의류 정보를 추출하는 엔진입니다.
            반드시 JSON으로만 응답하세요.
            스키마: { name: string, imageUrl: string|null, type: string, attributes: [ {definitionName: string, value: string} ] }
            type은 다음 중 하나만: TOP,BOTTOM,DRESS,OUTER,UNDERWEAR,ACCESSORY,SHOES,SOCKS,HAT,BAG,SCARF,ETC
            attributes.definitionName은 아래 허용 목록의 키 중 하나여야 하며, value는 해당 옵션 중 택1입니다(없으면 가장 근접 문자열).
            확실하지 않으면 null/빈 배열. 임의 추측 금지.
            """;
        if (allowedDefs != null && !allowedDefs.isEmpty()) {
            system += "\n허용 속성-옵션:";
            for (Map.Entry<String, List<String>> e : allowedDefs.entrySet()) {
                system += "\n- " + e.getKey() + ": " + String.join("|", e.getValue());
            }
        }
        String user = "URL: " + uri + "\nTITLE: " + title + "\n\nTEXT:\n" + text;

        return chatClient.chatJson(props.model(), system, user)
            .flatMap(content -> {
                try {
                    JsonNode root = om.readTree(content);
                    String name = txt(root, "name");
                    String imageUrl = firstNonBlank(txt(root, "imageUrl"), image);
                    String type = normalizeType(txt(root, "type"));
                    List<ExtractedClothing.ExtractedAttribute> attrs = new ArrayList<>();
                    if (root.has("attributes") && root.get("attributes").isArray()) {
                        for (JsonNode n : root.get("attributes")) {
                            String dn = txt(n, "definitionName");
                            String v = txt(n, "value");
                            if (dn != null && v != null)
                                attrs.add(new ExtractedClothing.ExtractedAttribute(dn, v));
                        }
                    }
                    return Mono.just(new ExtractedClothing(name, imageUrl, type, attrs));
                } catch (Exception e) {
                    return Mono.error(e);
                }
            });
    }


    private static String txt(JsonNode n, String f) {
        return n.has(f) && !n.get(f).isNull() ? n.get(f).asText() : null;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }


    private static String normalizeType(String raw){
        if (raw == null) return null; String r = raw.trim().toUpperCase();
        return switch (r){
            case "티셔츠", "상의" -> "TOP";
            case "바지", "팬츠", "하의" -> "BOTTOM";
            case "원피스" -> "DRESS";
            case "자켓", "재킷", "코트", "점퍼", "아우터" -> "OUTER";
            case "신발", "슈즈" -> "SHOES";
            case "모자" -> "HAT";
            case "가방" -> "BAG";
            case "양말" -> "SOCKS";
            case "스카프", "머플러" -> "SCARF";
            case "액세서리" -> "ACCESSORY";
            default -> r; // 이미 표준이면 그대로
        };
    }
}