package com.example.bitbucketstats.configuration;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

@Component
public class HttpRetryConfig {

  /**
   * Backoff for idempotent GETs; retries only transient failures (429/5xx, I/O timeouts).
   */
  @Bean
  public Retry httpRetry() {
    return Retry.backoff(3, Duration.ofSeconds(1))
        .jitter(0.2)
        .filter(HttpRetryConfig::isRetryable)
        .transientErrors(true);
  }

  /**
   * Check if the given exception is retryable. This includes WebClientResponseException with 429/5xx status codes, as
   * well as common transient I/O errors like IOException and TimeoutException.
   *
   * @param t the Throwable to check
   * @return true if the exception is retryable, false otherwise
   */
  public static boolean isRetryable(Throwable t) {
    if (t instanceof WebClientResponseException e) {
      int c = e.getStatusCode().value();
      return c == 429 || (c >= 500 && c < 600);
    }

    // Transport-level errors are often wrapped as WebClientRequestException
    if (t instanceof WebClientRequestException reqEx) {
      Throwable cause = reqEx.getCause();
      return cause instanceof java.io.IOException || cause instanceof java.util.concurrent.TimeoutException
          || cause instanceof reactor.netty.channel.AbortedException
          || cause instanceof io.netty.handler.timeout.ReadTimeoutException || cause == null;
    }

    // Direct throw of lower-level types (sometimes you’ll get these without wrapping)
    return t instanceof java.io.IOException || t instanceof java.util.concurrent.TimeoutException
        || t instanceof reactor.netty.channel.AbortedException
        || t instanceof io.netty.handler.timeout.ReadTimeoutException;
  }
}
