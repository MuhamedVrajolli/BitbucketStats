package com.example.bitbucketstats.integration.response;

import java.time.OffsetDateTime;
import java.util.List;

public record PullRequest(
    int id,
    String title,
    Author author,
    Integer commentCount,
    List<Participant> participants,
    OffsetDateTime createdOn,
    OffsetDateTime updatedOn
) {

}
