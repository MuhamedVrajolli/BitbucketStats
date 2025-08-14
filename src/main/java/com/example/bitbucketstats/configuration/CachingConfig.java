package com.example.bitbucketstats.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  public CacheManager cacheManager() {
    var caffeine = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofMinutes(30))
        .recordStats();
    var cacheManager = new CaffeineCacheManager();

    cacheManager.setCaffeine(caffeine);
    cacheManager.setCacheNames(List.of(BITBUCKET_USER_CACHE));
    cacheManager.setAsyncCacheMode(true);

    log.debug("Caffeine cache manager initialized (caches={})", cacheManager.getCacheNames());
    return cacheManager;
  }
}
