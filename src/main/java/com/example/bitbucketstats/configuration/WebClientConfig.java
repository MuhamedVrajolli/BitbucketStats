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
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebClientConfig {

  private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

  @Bean
  public ConnectionProvider connectionProvider() {
    // tune as you like
    return ConnectionProvider.builder("http-pool")
        .maxConnections(200)
        .pendingAcquireMaxCount(500)
        .pendingAcquireTimeout(Duration.ofSeconds(10))
        .maxIdleTime(Duration.ofSeconds(30))
        .maxLifeTime(Duration.ofMinutes(5))
        .build();
  }

  @Bean
  public HttpClient httpClient(ConnectionProvider provider) {
    return HttpClient.create(provider)
        .compress(true)
        .followRedirect(true)
        .responseTimeout(Duration.ofSeconds(30))
        .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
        .doOnConnected(conn -> conn
            .addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(30))
            .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(30)));
  }

  @Bean
  public WebClient webClient(ObjectMapper objectMapper,
      HttpClient httpClient) {
    var strategies = ExchangeStrategies.builder()
        .codecs(cfg -> {
          cfg.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
          cfg.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
        })
        .build();

    ExchangeFilterFunction reqLog = ExchangeFilterFunction.ofRequestProcessor(req -> {
      // redact auth & long query params
      var uri = req.url();
      log.trace("--> {} {} (headers redacted)", req.method(), uri.getPath());
      return Mono.just(req);
    });

    ExchangeFilterFunction resLog = ExchangeFilterFunction.ofResponseProcessor(res -> {
      log.trace("<-- {} (headers={})", res.statusCode(), res.headers().asHttpHeaders().keySet());
      return Mono.just(res);
    });

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(strategies)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .filter(reqLog)
        .filter(resLog)
        .build();
  }
}
