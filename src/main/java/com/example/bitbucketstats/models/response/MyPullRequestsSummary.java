package com.example.bitbucketstats.models.response;

import com.example.bitbucketstats.models.DiffDetails;

public record MyPullRequestsSummary(
    int id,
    String title,
    String link,
    Integer timeOpenHours,
    Integer commentCount,
    String repo,
    DiffDetails diffDetails
) {

}
