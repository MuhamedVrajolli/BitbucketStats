package com.example.bitbucketstats.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CachingConfig {

  public static final String BITBUCKET_USER_CACHE = "bitbucket-user-cache";
  private static final Logger log = LoggerFactory.getLogger(CachingConfig.class);

  @Bean
  @ConditionalOnMissingBean(Ticker.class)
  public Ticker caffeineTicker() {
    return Ticker.systemTicker();
  }

  @Bean
  public CacheManager cacheManager(Ticker ticker) {
    var caffeine = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(java.time.Duration.ofMinutes(30))
        .recordStats()
        .ticker(ticker);

    var mgr = new CaffeineCacheManager();
    mgr.setCaffeine(caffeine);
    mgr.setCacheNames(java.util.List.of(BITBUCKET_USER_CACHE));
    mgr.setAsyncCacheMode(true);
    log.debug("Caffeine cache manager initialized (caches={})", mgr.getCacheNames());
    return mgr;
  }
}
