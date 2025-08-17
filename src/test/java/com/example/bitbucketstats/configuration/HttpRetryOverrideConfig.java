package com.example.bitbucketstats.configuration;

import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import reactor.util.retry.Retry;

@TestConfiguration
public class HttpRetryOverrideConfig {

  @Bean
  public Retry httpRetry() {
    return Retry.backoff(3, Duration.ZERO)
        .jitter(0.0)
        .filter(HttpRetryConfig::isRetryable);
  }
}
