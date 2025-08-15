package com.example.bitbucketstats.models.response;

public record PullRequestCommentSummary(
    int id,
    String title,
    String link,
    int commentsMade,
    String repo) {

}
