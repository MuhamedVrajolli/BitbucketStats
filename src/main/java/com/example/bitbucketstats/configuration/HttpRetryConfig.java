package com.example.bitbucketstats.configuration;

import java.time.Duration;
import lombok.NoArgsConstructor;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class HttpRetryConfig {

  /**
   * Backoff for idempotent GETs; retries only transient failures (429/5xx, I/O timeouts).
   */
  public static final Retry RETRY_POLICY = Retry.backoff(3, Duration.ofSeconds(1))
      .jitter(0.2)
      .filter(HttpRetryConfig::isRetryable)
      .transientErrors(true);

  /**
   * Check if the given exception is retryable.
   * This includes WebClientResponseException with 429/5xx status codes,
   * as well as common transient I/O errors like IOException and TimeoutException.
   *
   * @param t the Throwable to check
   * @return true if the exception is retryable, false otherwise
   */
  public static boolean isRetryable(Throwable t) {
    if (t instanceof WebClientResponseException e) {
      int c = e.getStatusCode().value();
      return c == 429 || (c >= 500 && c < 600);
    }
    return t instanceof java.io.IOException || t instanceof java.util.concurrent.TimeoutException
        || t instanceof reactor.netty.channel.AbortedException;
  }
}
