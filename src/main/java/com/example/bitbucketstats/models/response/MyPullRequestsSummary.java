package com.example.bitbucketstats.models.response;

public record MyPullRequestsSummary(
    String title,
    String link,
    Integer timeOpenHours,
    Integer commentCount
) {

}
