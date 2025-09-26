package org.ikuzo.otboo.domain.clothes.extractions;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiChatClient {

    private final WebClient openAiWebClient;

    public Mono<String> chatJson(String model, String system, String user, double temperature) {

        log.info("[OpenAiChatClient] chatJson - model: {}, temperature: {}", model, temperature);

        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
            ),
            "response_format", Map.of("type", "json_object"),
            "temperature", temperature
        );

        return openAiWebClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .onStatus(HttpStatusCode::isError,
                resp -> resp.bodyToMono(String.class)
                    .map(error -> new RuntimeException("OpenAi error: " + error)))
            .bodyToMono(JsonNode.class)
            .map(json -> {
                JsonNode n = json.at("/choices/0/message/content");
                if (n.isMissingNode() || n.isNull() || n.asText().isBlank()) {
                    throw new IllegalStateException("Empty content in OpenAI response: " + json);
                }
                return n.asText();
            });
    }
}
