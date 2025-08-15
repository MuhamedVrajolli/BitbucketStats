package com.example.bitbucketstats.models.response;

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
