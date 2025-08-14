package com.example.bitbucketstats.models;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RawPullRequest(
    int id,
    String title,
    Author author,
    Integer commentCount,
    List<Participant> participants,
    java.time.OffsetDateTime createdOn,
    java.time.OffsetDateTime updatedOn
) {

}
