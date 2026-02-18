package com.example.bitbucketstats.controllers.response;

import java.util.List;

public record PullRequestReviewResponse(
    String period,
    int totalPullRequestsApproved,
    int totalPullRequestsReviewed,
    Integer totalPullRequestsCommented,
    Integer totalComments,
    Double approvedPercentage,
    Double commentedPercentage,
    List<PullRequestCommentSummary> pullRequestsCommented
) {

}
