package com.example.bitbucketstats.itests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.bitbucketstats.configuration.CachingConfig;
import com.example.bitbucketstats.configuration.CachingOverrideConfig;
import com.example.bitbucketstats.configuration.CachingOverrideConfig.ManualTicker;
import com.example.bitbucketstats.integration.BitBucketService;
import com.example.bitbucketstats.models.BitbucketAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

@Tag("integration")
@Import(CachingOverrideConfig.class) // bring in FakeTicker + short TTL
class GetUserCacheIT extends BaseIT {

  @Autowired ObjectMapper om;
  @Autowired BitBucketService bitBucketService;
  @Autowired CacheManager cacheManager;
  @Autowired Ticker ticker; // FakeTicker

  @BeforeEach
  void clearCaches() {
    Objects.requireNonNull(cacheManager.getCache(CachingConfig.BITBUCKET_USER_CACHE)).clear();
  }

  @Test
  void caches_on_first_subscription_then_hits_cache() throws Exception {
    stubUser("{abc}", "John");

    var auth = new BitbucketAuth("Basic auth", "john", "pass");

    // 1st call — goes to WireMock
    StepVerifier.create(bitBucketService.getCurrentUser(auth))
        .assertNext(u -> assertThat(u.uuid()).isEqualTo("{abc}"))
        .verifyComplete();

    // 2nd call — should be served from cache (no new upstream call)
    StepVerifier.create(bitBucketService.getCurrentUser(auth))
        .assertNext(u -> assertThat(u.uuid()).isEqualTo("{abc}"))
        .verifyComplete();

    // Only one upstream hit
    WireMock.verify(1, getRequestedFor(urlEqualTo("/user")));

    // Check Caffeine stats
    var nativeCache = ((CaffeineCache)
        Objects.requireNonNull(cacheManager.getCache(CachingConfig.BITBUCKET_USER_CACHE)))
        .getNativeCache();
    var stats = nativeCache.stats();
    assertThat(stats.hitCount()).isGreaterThanOrEqualTo(1);
    assertThat(stats.missCount()).isEqualTo(1);
  }

  @Test
  void different_key_does_not_use_cache() throws Exception {
    // Return same payload; we only verify request count by different auth keys
    stubUser("{xyz}", "Jane");

    var a1 = new BitbucketAuth("Basic cache1", "john", "pass");  // cache key #1
    var a2 = new BitbucketAuth("Basic cache2", "jane", "pass");  // cache key #2

    StepVerifier.create(bitBucketService.getCurrentUser(a1)).expectNextCount(1).verifyComplete();
    StepVerifier.create(bitBucketService.getCurrentUser(a2)).expectNextCount(1).verifyComplete();

    // Two upstream calls because keys differ
    WireMock.verify(2, getRequestedFor(urlEqualTo("/user")));
  }

  @Test
  void expires_after_ttl_and_refetches() throws Exception {
    stubUser("{abc}", "John");
    var auth = new BitbucketAuth("Basic auth", "john", "pass");

    // Prime cache
    StepVerifier.create(bitBucketService.getCurrentUser(auth)).expectNextCount(1).verifyComplete();
    WireMock.verify(1, getRequestedFor(urlEqualTo("/user")));

    // Advance FakeTicker past TTL (1s)
    ((ManualTicker) ticker).advance(Duration.ofSeconds(2));

    // Next call should MISS and fetch again
    StepVerifier.create(bitBucketService.getCurrentUser(auth)).expectNextCount(1).verifyComplete();
    WireMock.verify(2, getRequestedFor(urlEqualTo("/user")));
  }

  private void stubUser(String uuid, String displayName) throws Exception {
    stubFor(get(urlEqualTo("/user"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(om.writeValueAsString(Map.of(
                "uuid", uuid,
                "display_name", displayName
            )))));
  }
}

