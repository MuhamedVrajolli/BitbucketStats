package com.example.bitbucketstats.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class CachingOverrideConfig {

  @Bean
  @Primary
  public ManualTicker testTicker() {
    return new ManualTicker();
  }

  // Shorten TTL for faster expiry tests
  @Bean
  @Primary
  public CacheManager testCacheManager(ManualTicker manualTicker) {
    var caffeine = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(java.time.Duration.ofSeconds(1)) // short TTL for tests
        .recordStats()
        .ticker(manualTicker);

    var mgr = new CaffeineCacheManager();
    mgr.setCaffeine(caffeine);
    mgr.setCacheNames(java.util.List.of(CachingConfig.BITBUCKET_USER_CACHE));
    mgr.setAsyncCacheMode(true);
    return mgr;
  }

  public static class ManualTicker implements Ticker {

    private final AtomicLong nanos = new AtomicLong();

    @Override
    public long read() {
      return nanos.get();
    }

    public void advance(Duration d) {
      nanos.addAndGet(d.toNanos());
    }
  }
}

