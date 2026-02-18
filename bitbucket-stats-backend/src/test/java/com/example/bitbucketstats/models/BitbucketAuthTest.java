package com.example.bitbucketstats.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;

@Tag("unit")
class BitbucketAuthTest {

  @Test
  void fromHeaders_basicHeader_isParsed() {
    var auth = BitbucketAuth.fromHeaders("Basic abc123==", null, null);
    assertThat(auth.basicValue()).isEqualTo("abc123==");
    assertThat(auth.user()).isNull();
    assertThat(auth.appPassword()).isNull();

    var headers = new HttpHeaders();
    auth.apply(headers);
    assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Basic abc123==");
    assertThat(auth.cacheKey()).isEqualTo("abc123==");
  }

  @Test
  void fromHeaders_usernameAndAppPassword_areEncoded() {
    var auth = BitbucketAuth.fromHeaders(null, "alice", "s3cr3t");
    var expected = Base64.getEncoder()
        .encodeToString("alice:s3cr3t".getBytes(StandardCharsets.UTF_8));

    assertThat(auth.basicValue()).isEqualTo(expected);
    assertThat(auth.user()).isEqualTo("alice");
    assertThat(auth.appPassword()).isEqualTo("s3cr3t");
    assertThat(auth.cacheKey()).isEqualTo(expected);
  }

  @Test
  void fromHeaders_missingData_throws() {
    assertThatThrownBy(() -> BitbucketAuth.fromHeaders(null, null, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Provide either Authorization: Basic ... or username+appPassword");
  }
}
