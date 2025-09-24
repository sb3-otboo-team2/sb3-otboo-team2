package org.ikuzo.otboo.domain.clothes.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(OpenAiProps.class)
public class OpenAiClientConfig {

    @Bean
    WebClient openAiWebClient(WebClient.Builder builder, OpenAiProps props) {
        HttpClient http = HttpClient.create()
            .responseTimeout(Duration.ofMillis(props.timeoutMs()))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);


        return builder
            .baseUrl(props.baseUrl())
            .clientConnector(new ReactorClientHttpConnector(http))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
            .build();
    }
}
