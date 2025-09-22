package org.ikuzo.otboo.domain.clothes.parser;

import java.net.URI;

public interface HtmlParserResolver {

    Parsed parse(URI uri);

    /**
     * @param fullHtml  최종 응답의 전체 HTML
     * @param finalUrl  리다이렉트 이후 최종 URL(없으면 원본)
     * @param title     og:title 또는 <title>
     * @param ogImage   og:image (없으면 null)
     * @param contentType 응답 Content-Type (text/html 등)\
     * @param strippedText script/style/noscript 제거 후 텍스트
     */
    record Parsed(
        String fullHtml,
        String finalUrl,
        String title,
        String ogImage,
        String contentType,
        String strippedText
    ) {}
}
