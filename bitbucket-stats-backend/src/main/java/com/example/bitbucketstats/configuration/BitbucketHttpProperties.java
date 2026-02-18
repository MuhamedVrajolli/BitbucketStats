package com.example.bitbucketstats.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bitbucket")
public record BitbucketHttpProperties(
    String apiBase
    // Optional: String username, String appPassword,
    // Optional: Duration connectTimeout, responseTimeout, etc.
) {

}
