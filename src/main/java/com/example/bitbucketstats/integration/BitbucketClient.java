package com.example.bitbucketstats.integration;

import static com.example.bitbucketstats.configuration.HttpRetryConfig.RETRY_POLICY;

import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.NextPage;
import com.example.bitbucketstats.configuration.HttpRetryConfig;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BitbucketClient {

  private static final Logger log = LoggerFactory.getLogger(BitbucketClient.class);

  private final WebClient webClient;

  public <T extends NextPage> Flux<T> fetchPaged(BitbucketAuth auth, String firstUrl, Class<T> type) {
    log.trace("fetchPaged start: {} (type={})", firstUrl, type.getSimpleName());
    return Mono.defer(() -> retrieveJson(auth, firstUrl, type))
        .expand(page -> {
          if (page.next() == null) return Mono.empty();
          log.trace("Paging next: {}", page.next());
          return retrieveJson(auth, page.next(), type);
        })
        .doOnComplete(() -> log.trace("fetchPaged complete for {}", firstUrl));
  }

  public <T> Mono<T> retrieveJson(BitbucketAuth auth, String url, Class<T> type) {
    log.trace("HTTP GET {}", url);
    return webClient.get()
        .uri(URI.create(url))
        .headers(auth::apply)
        .retrieve()
        .onStatus(HttpRetryConfig::isRetryableErrorStatus, ClientResponse::createException)
        .bodyToMono(type)
        .doOnSuccess(body -> {
          if (body instanceof NextPage np) {
            log.trace("Fetched page type={} next={}", type.getSimpleName(), np.next());
          } else {
            log.trace("Fetched object type={}", type.getSimpleName());
          }
        })
        .doOnError(e -> log.warn("Request failed for {}", url, e))
        .retryWhen(RETRY_POLICY);
  }
}
