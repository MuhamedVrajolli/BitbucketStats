package com.example.bitbucketstats.controllers.response;

import com.example.bitbucketstats.models.DiffDetails;
import java.time.OffsetDateTime;

public record MyPullRequestsSummary(
    int id,
    String title,
    String link,
    Integer timeOpenHours,
    Integer commentCount,
    String repo,
    DiffDetails diffDetails,
    OffsetDateTime createdOn,
    OffsetDateTime closedOn
) {

}

