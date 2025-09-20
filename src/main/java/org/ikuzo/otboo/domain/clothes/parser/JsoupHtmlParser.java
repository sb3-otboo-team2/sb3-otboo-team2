package org.ikuzo.otboo.domain.clothes.parser;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsoupHtmlParser implements HtmlParserResolver {

    private static final int TIMEOUT_MS = 7000;
    private static final String UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    @Override
    public Parsed parse(URI uri) {
        try {
            Connection.Response res = Jsoup.connect(uri.toString())
                .userAgent(UA)
                .ignoreContentType(true)
                .followRedirects(true)
                .timeout(TIMEOUT_MS)
                .method(Connection.Method.GET)
                .execute();

            String finalUrl = Optional.ofNullable(res.url()).map(Object::toString).orElse(uri.toString());
            String contentType = Optional.ofNullable(res.contentType()).orElse("text/html");

            Document doc = res.parse();
            doc.select("script, style, noscript").remove();

            String title = Optional.ofNullable(doc.selectFirst("meta[property=og:title]"))
                .map(e -> e.attr("content")).filter(s -> !s.isBlank())
                .orElse(doc.title());

            String ogImage = Optional.ofNullable(doc.selectFirst("meta[property=og:image]"))
                .map(e -> e.attr("content")).filter(s -> !s.isBlank())
                .orElse(null);

            return new Parsed(doc.outerHtml(), finalUrl, title, ogImage, contentType);

        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch/parse HTML: " + uri, e);
        }
    }
}
