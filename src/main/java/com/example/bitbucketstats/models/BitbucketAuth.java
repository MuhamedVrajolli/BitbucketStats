package com.example.bitbucketstats.models;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

public record BitbucketAuth(String basicValue, String user, String appPassword) {
  public static BitbucketAuth fromHeaders(String authorization, String username, String appPassword) {
    if (authorization != null && authorization.toLowerCase(Locale.ROOT).startsWith("basic ")) {
      return new BitbucketAuth(authorization.substring(6).trim(), null, null);
    } else if (StringUtils.hasText(username) && StringUtils.hasText(appPassword)) {
      String token = Base64.getEncoder().encodeToString((username + ":" + appPassword).getBytes(StandardCharsets.UTF_8));
      return new BitbucketAuth(token, username, appPassword);
    } else {
      throw new IllegalArgumentException("Provide either Authorization: Basic ... or username+appPassword headers");
    }
  }

  public void apply(HttpHeaders h) {
    h.set(HttpHeaders.AUTHORIZATION, "Basic " + basicValue);
  }

  public String cacheKey() {
    return basicValue;
  }
}
