package com.example.bitbucketstats.models;

import com.example.bitbucketstats.integration.response.Participant;
import com.example.bitbucketstats.integration.response.PullRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Enriched PR used internally
 */
public record EnrichedPullRequest(
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

  public static EnrichedPullRequest from(PullRequest r, String repo) {
    return new EnrichedPullRequest(
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
