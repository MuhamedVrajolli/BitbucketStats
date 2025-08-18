package com.example.bitbucketstats.controllers.response;

public record PullRequestCommentSummary(
    int id,
    String title,
    String link,
    int commentsMade,
    String repo
) {

}
