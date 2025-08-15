package com.example.bitbucketstats.models.response;

import java.util.List;

public record MyPullRequestsResponse(
    String period,
    int totalPullRequests,
    Double avgTimeOpenHours,
    Double avgCommentCount,
    Double avgFilesChanged,
    List<MyPullRequestsSummary> pullRequestDetails
) {

}
