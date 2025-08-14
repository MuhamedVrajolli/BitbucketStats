package com.example.bitbucketstats.models;

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
    java.time.OffsetDateTime createdOn,
    java.time.OffsetDateTime updatedOn
) {

  public boolean approvedBy(String myUuid) {
    if (participants == null) {
      return false;
    }
    return participants.stream().filter(Objects::nonNull)
        .anyMatch(p -> p.user() != null && myUuid.equals(p.user().uuid()) && Boolean.TRUE.equals(p.approved()));
  }

  public PullRequest withRepo(String repo) {
    return new PullRequest(id, title, authorUuid, commentCount, participants, repo, createdOn, updatedOn);
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
