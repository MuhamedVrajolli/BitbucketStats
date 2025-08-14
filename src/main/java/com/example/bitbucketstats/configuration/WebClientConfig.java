package com.example.bitbucketstats.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

  private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

  @Bean
  public WebClient webClient(ObjectMapper objectMapper) {
    // Reactor Netty with timeouts and connection pool
    HttpClient httpClient = HttpClient.create()
        .responseTimeout(Duration.ofSeconds(30))
        .compress(true)
        .followRedirect(true)
        .keepAlive(true);

    // Ensure WebClient uses the same ObjectMapper as Spring Boot (with snake_case strategy)
    var strategies = ExchangeStrategies.builder()
        .codecs(cfg -> {
          cfg.defaultCodecs()
              .jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
          cfg.defaultCodecs()
              .jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
        })
        .build();

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(strategies)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .filter(ExchangeFilterFunction.ofRequestProcessor(req -> {
          log.trace("--> {} {}", req.method(), req.url());
          return Mono.just(req);
        }))
        .filter(ExchangeFilterFunction.ofResponseProcessor(res -> {
          log.trace("<-- {} (headers={})", res.statusCode(), res.headers().asHttpHeaders());
          return Mono.just(res);
        }))
        .build();
  }
}
