package com.example.bitbucketstats.models;

import java.time.OffsetDateTime;
import java.util.List;

public record RawPullRequest(
    int id,
    String title,
    Author author,
    Integer commentCount,
    List<Participant> participants,
    OffsetDateTime createdOn,
    OffsetDateTime updatedOn
) {

}
