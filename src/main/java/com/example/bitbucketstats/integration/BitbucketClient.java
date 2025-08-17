package com.example.bitbucketstats.integration;

import static com.example.bitbucketstats.utils.GeneralUtils.isUrlAbsolute;

import com.example.bitbucketstats.configuration.BitbucketHttpProperties;
import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.page.Page;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(BitbucketHttpProperties.class)
public class BitbucketClient {

  private static final Logger log = LoggerFactory.getLogger(BitbucketClient.class);

  private final Retry retryPolicy;
  private final WebClient webClient;
  private final BitbucketHttpProperties bitbucketHttpProperties;

  /**
   * Fetch all pages of results from a paginated Bitbucket API endpoint.
   *
   * @param auth the authentication details
   * @param firstUrl the URL of the first page
   * @param pageType the type of page to retrieve
   * @param <E> the type of elements in the page
   * @param <P> the type of page
   * @return a Flux that emits all elements across all pages
   */
  public <E, P extends Page<E>> Flux<E> fetchAll(BitbucketAuth auth, String firstUrl, Class<P> pageType) {
    log.trace("fetchAll start: {} (type={})", firstUrl, pageType.getSimpleName());

    return Mono.defer(() -> retrieveJson(auth, firstUrl, pageType))
        .expand(page -> {
          var next = page.next();
          if (next == null) {
            return Mono.empty();
          }
          log.trace("Paging next: {}", next);
          return retrieveJson(auth, next, pageType);
        })
        .flatMapIterable(p -> p.values() == null ? List.of() : p.values())
        .doOnComplete(() -> log.trace("fetchAll complete for {}", firstUrl));
  }

  /**
   * Retrieve an object from the Bitbucket API.
   *
   * @param auth the authentication details
   * @param url the URL to fetch
   * @param type the class type of the expected response
   * @param <T> the type of the response
   * @return a Mono that emits the fetched object
   */
  public <T> Mono<T> retrieveJson(BitbucketAuth auth, String url, Class<T> type) {
    String finalUrl = isUrlAbsolute(url) ? url : bitbucketHttpProperties.apiBase() + url;
    log.trace("HTTP GET {}", finalUrl);
    return webClient.get()
        .uri(URI.create(finalUrl))
        .headers(auth::apply)
        .retrieve()
        .onStatus(HttpStatusCode::isError, ClientResponse::createException)
        .bodyToMono(type)
        .doOnSuccess(body -> log.trace("Fetched object type={}", type.getSimpleName()))
        .doOnError(e -> log.warn("Request failed for {}", url, e))
        .retryWhen(retryPolicy);
  }
}
