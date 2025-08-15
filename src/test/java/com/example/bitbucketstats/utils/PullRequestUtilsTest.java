package com.example.bitbucketstats.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bitbucketstats.models.EnrichedPullRequest;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PullRequestUtilsTest {

  // helper to build only what we need for each case
  private static EnrichedPullRequest pr(String repo, int id, OffsetDateTime created, OffsetDateTime updated) {
    return new EnrichedPullRequest(id, null, null, null, null,
        repo, created, updated
    );
  }

  @Test
  void hoursOpen_returns0_whenEitherTimestampIsNull() {
    var created = OffsetDateTime.parse("2025-08-01T10:00:00Z");
    var updated = OffsetDateTime.parse("2025-08-01T12:00:00Z");

    assertThat(PullRequestUtils.hoursOpen(pr("svc-a", 1, null, updated))).isEqualTo(0);
    assertThat(PullRequestUtils.hoursOpen(pr("svc-a", 1, created, null))).isEqualTo(0);
  }

  @Test
  void hoursOpen_truncatesToWholeHours() {
    var created = OffsetDateTime.parse("2025-08-01T10:00:00Z");
    var updated = OffsetDateTime.parse("2025-08-01T11:59:00Z"); // 1h59m -> 1h
    assertThat(PullRequestUtils.hoursOpen(pr("svc-a", 1, created, updated))).isEqualTo(1);
  }

  @Test
  void hoursOpen_handlesCrossDayDurations() {
    var created = OffsetDateTime.parse("2025-08-01T10:00:00Z");
    var updated = OffsetDateTime.parse("2025-08-02T12:00:00Z"); // +26h
    assertThat(PullRequestUtils.hoursOpen(pr("svc-a", 1, created, updated))).isEqualTo(26);
  }

  @Test
  void prLink_buildsExpectedUrl() {
    assertThat(PullRequestUtils.prLink("acme", "svc-a", 123))
        .isEqualTo("https://bitbucket.org/acme/svc-a/pull-requests/123");
  }

  @Test
  void prKey_isRepoHashId() {
    assertThat(PullRequestUtils.prKey(pr("svc-b", 456, null, null)))
        .isEqualTo("svc-b#456");
  }
}
