package com.example.bitbucketstats.models;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Enriched PR used internally
 */
public record PullRequest(
    int id,
    String title,
    String authorUuid,
    Integer commentCount,
    List<Participant> participants,
    String repo,
    OffsetDateTime createdOn,
    OffsetDateTime updatedOn
) {

  public boolean approvedBy(String myUuid) {
    if (participants == null) {
      return false;
    }
    return participants.stream().filter(Objects::nonNull)
        .anyMatch(p -> p.user() != null && myUuid.equals(p.user().uuid()) && Boolean.TRUE.equals(p.approved()));
  }

  public static PullRequest from(RawPullRequest r, String repo) {
    return new PullRequest(
        r.id(),
        r.title(),
        r.author() == null ? null : r.author().uuid(),
        r.commentCount(),
        r.participants(),
        repo,
        r.createdOn(),
        r.updatedOn()
    );
  }
}
